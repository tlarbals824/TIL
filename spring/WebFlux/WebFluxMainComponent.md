## WebFlux 핵심 컴포넌트

### Httphandler

HttpHandler는 다른 유형의 HTTP 서버 API로 request와 response를 처리하기 위해 추상화된 단 하나의 메서드만 가집니다.

~~~java
public interface HttpHandler {
	Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response);
}
~~~

HttpHandler의 구현체인 HttpWebHandlerAdapter클래스는 handle() 메서드의 파라미터로 전달받은 ServerHttpRequest와 ServerHttpResonse로 ServerWebExchange를 생성한 후에 WebHandler를 호출하는 역할을 합니다.

~~~java
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {
    ...

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        ...
        ServerWebExchange exchange = createExchange(request, response);
        ...
        return getDelegate().handle(exchange)
            .doOnSuccess(aVoid -> logResponse(exchange))
            .onErrorResume(ex -> handleUnresolvedError(exchange, ex))
            .then(exchange.cleanupMultipart())
            .then(Mono.defer(response::setComplete));
    }
}
~~~

### WebFilter

WebFilter는 Spring MVC의 Servlet Filter처럼 Handler가 요청을 처리하기 전에 전처리 작업을 할 수 있도록 해 줍니다.

WebFilter는 주로 보안이나 세션 타임아웃 처리 등 애플리케이션에서 공통으로 필요한 전처리에 사용됩니다. WebFilter는 filter(ServerWebExchange exchange, WebFilterChain chain) 메서드로 정의되어 있으며, 파라미터로 전달받은 WebFilterChain을 통해 필터 체인을 형성하여 원하는 만큼의 WebFilter를 추가할 수 있습니다.


~~~java
public interface WebFilter {
	Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain);
}
~~~

WebFilter를 구현하여 요청 URI에 대해 로그로 출력하는 클래스를 만들어보았습니다. 

~~~java
@Component
@Slf4j
public class LogWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.info("URL : {}",exchange.getRequest().getURI().getPath());
        return chain.filter(exchange);
    }
}
~~~

### HandlerFilterFunction

HandlerFilterFunction은 함수형 기반의 요청 핸들러에 적용할 수 있는 Filter입니다.

HandlerFilterFunction은 filter(ServerRequest request, HandlerFunction<T> next) 메서드로 정의되어 있으며, 파라미터로 전달받은 HandlerFunction에 연결됩니다.

HandlerFilterFunction에 경우 WebFilter와는 다르게 애노테이션 기반 핸들러가 아닌 함수형 기반의 요청 핸들러에서 함수 형태로 사용되기 때문에 Spring Bean으로 등록되지 않습니다.

~~~java
@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {
    Mono<R> filter(ServerRequest request, HandlerFunction<T> next);
    ...
}
~~~

### DispatcherHandler

DispatcherHandler는 WebHandler 인터페이스의 구현체로서 Spring MVC에서 Front Controller 패턴이 적용된 DispatcherSevlet처럼 중앙에서 먼저 요청을 전달받을 후에 다른 컴포넌트에 요청 처리를 위임합니다.

DispatcherHandler 자체가 Spring Bean으로 등록되도록 설계되었으며, ApplicationContext에서 HandlerMapping, HandlerAdapter, HandlerResultHandler 등의 요청 처리를 위한 위임 컴포넌트를 검색합니다.

~~~java
public class DispatcherHandler implements WebHandler, PreFlightRequestHandler, ApplicationContextAware {
    @Nullable
    private List<HandlerMapping> handlerMappings;

    @Nullable
    private List<HandlerAdapter> handlerAdapters;

    @Nullable
    private List<HandlerResultHandler> resultHandlers;

    protected void initStrategies(ApplicationContext context) {
        Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, HandlerMapping.class, true, false);

        ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());
        AnnotationAwareOrderComparator.sort(mappings);
        this.handlerMappings = Collections.unmodifiableList(mappings);

        Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, HandlerAdapter.class, true, false);

        this.handlerAdapters = new ArrayList<>(adapterBeans.values());
        AnnotationAwareOrderComparator.sort(this.handlerAdapters);

        Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, HandlerResultHandler.class, true, false);

        this.resultHandlers = new ArrayList<>(beans.values());
        AnnotationAwareOrderComparator.sort(this.resultHandlers);
    }
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        if (this.handlerMappings == null) {
            return createNotFoundError();
        }
        if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
            return handlePreFlight(exchange);
        }
        return Flux.fromIterable(this.handlerMappings)
            .concatMap(mapping -> mapping.getHandler(exchange))
            .next()
            .switchIfEmpty(createNotFoundError())
            .onErrorResume(ex -> handleDispatchError(exchange, ex))
            .flatMap(handler -> handleRequestWith(exchange, handler));
    }
    ...
    private Mono<Void> handleRequestWith(ServerWebExchange exchange, Object handler) {
        if (ObjectUtils.nullSafeEquals(exchange.getResponse().getStatusCode(), HttpStatus.FORBIDDEN)) {
            return Mono.empty();  // CORS rejection
        }
        if (this.handlerAdapters != null) {
            for (HandlerAdapter adapter : this.handlerAdapters) {
                if (adapter.supports(handler)) {
                    return adapter.handle(exchange, handler)
                        .flatMap(result -> handleResult(exchange, result));
                }
            }
        }
        return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
    }
    
    private Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        Mono<Void> resultMono = doHandleResult(exchange, result, "Handler " + result.getHandler());
        if (result.getExceptionHandler() != null) {
            resultMono = resultMono.onErrorResume(ex ->
                result.getExceptionHandler().handleError(exchange, ex).flatMap(result2 ->
                    doHandleResult(exchange, result2, "Exception handler " +
                        result2.getHandler() + ", error=\"" + ex.getMessage() + "\"")));
        }
        return resultMono;
    }

    private Mono<Void> doHandleResult(
        ServerWebExchange exchange, HandlerResult handlerResult, String description) {

        if (this.resultHandlers != null) {
            for (HandlerResultHandler resultHandler : this.resultHandlers) {
                if (resultHandler.supports(handlerResult)) {
                    description += " [DispatcherHandler]";
                    return resultHandler.handleResult(exchange, handlerResult).checkpoint(description);
                }
            }
        }
        return Mono.error(new IllegalStateException(
            "No HandlerResultHandler for " + handlerResult.getReturnValue()));
    }
}
~~~

### HandlerMapping

HandlerMapping은 Spring MVC에서와 마찬가지로 request와 handler object에 대한 매핑을 정의하는 인터페이스이며, HandlerMapping 인터페이스를 구현하는 구현 클래스로는 RequestMappingHandlerMapping, RounterFunctionMapping 등이 있습니다.

HandlerMapping 인터페이스는 getHandler(ServerWebExchange exchange) 추상 메서드 하나만 정의되어 있으며 getHandler() 메서드는 파라미터로 입력받은 ServerWebExchange에 매치되는 handler object를 리턴합니다.

~~~java
public interface HandlerMapping {
    ...
    Mono<Object> getHandler(ServerWebExchange exchange);
}
~~~

### HandlerAdapter

HandlerAdapter는 HandlerMapping을 통해 얻은 핸들러를 직접적으로 호출하는 역할을 하며, 응답 결과로 Mono<HandlerResult>를 리턴받습니다.

Spring 5.0 Reactive 스택에서 지원하는 HandlerAdapter 구현 클래스로 RequestMappingHandlerAdapter, HandlerFunctionAdapter, SimpleHandlerAdapter, WebSocketHandlerAdapter가 있습니다.

* support : 파라미터로 입력받은 handler를 지원하는지 체크합니다.
* handle : 파라미터로 받은 handler를 이용하여 handler를 호출합니다.

~~~java
public interface HandlerAdapter {
    boolean supports(Object handler);
    Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);
}
~~~

~~~java
public class SimpleHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return WebHandler.class.isAssignableFrom(handler.getClass());
	}

	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebHandler webHandler = (WebHandler) handler;
		Mono<Void> mono = webHandler.handle(exchange);
		return mono.then(Mono.empty());
	}

}
~~~

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)