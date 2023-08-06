## WebFlux Controller

Spring WebFlux는 두 가지 프로그래밍 모델을 지원합니다.
1. 애노테이션 기반 프로그래밍
2. 함수형 기반 프로그래밍 모델

## WebFlux 애노테이션 기반 프로그래밍

WebFlux의 애노테이션 기반 프로그래밍은 Spring MVC의 애노테이션 프로그래밍 방식과 유사합니다. MVC와는 다르게 WebFlux의 Controller는 Non-Blocking을 지원하는 리액티브 핸들러입니다.

~~~java
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {
    private final LogService logService;

    @PostMapping()
    public Mono<Void> saveLog(@RequestBody SaveLogRequest request) {
        return logService.saveLog(request);
    }
    ...
}
~~~
~~~java
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {
    private final LogRepository logRepository;

    public Mono<Void> saveLog(SaveLogRequest request) {
        return logRepository.save(LogMapper.mapToLog(request)).then();
    }
    ...
}
~~~

## WebFlux 함수형 기반 프로그래밍

WebFlux의 함수형 엔드포인트는 애노테이션 기반 Controller와 달리 엔드포인트로 들어오는 요청을 처리하는 모든 과정이 하나의 함수 체인에서 처리합니다.

### HandlerFunction을 사용한 request 처리

WebFlux의 함수형 엔드포인트는 들어오는 요청을 처리하기 위해 HandlerFunction이라는 함수형 기반 핸들러를 사용합니다. 이는 Spring MVC에서 Controller의 Business Logic을 담당하는 Service에 해당합니다.

ServerRequest는 HandlerFunction에 의해 처리되는 HTTP request를 표현합니다. 이 ServerRequest의 객체를 통해 헤더, 메소드, URI, 쿼리 파라미터에 접근할 수 있습니다. 또한 body에 접근하기 위한 메소드들을 지원합니다.

ServerResponse는 HandlerFunction 또는 HandlerFilterFunction에서 리턴되는 HTTP response를 표현합니다. BodyBuilder와 HeadersBuilder를 통해 HTTP response body와 header 정보를 추가할 수 있습니다.

~~~java
@FunctionalInterface
public interface HandlerFunction<T extends ServerResponse> {
	Mono<T> handle(ServerRequest request);
}
~~~


### request 라우팅을 위한 RouterFunction

RouterFunction은 들어오는 요청을 해당 HandlerFunction으로 라우팅해 주는 역할을 합니다.

RouterFunction은 애노테이션 기반 프로그래밍의 @RequestMapping과 동일한 기능을 합니다.

RouterFunction은 요청을 위한 데이터뿐만 아니라 요청 처리를 위한 동작(HandlerFunction)까지 RouterFunction 파라미터로 제공한다는 차이점이 있습니다.

~~~java
@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {
    Mono<HandlerFunction<T>> route(ServerRequest request);
}
~~~

위에서의 애노테이션 기반 프로그래밍 로직을 함수형 기반으로 바꾸면 다음과 같습니다.

~~~java
@Configuration
public class LogRouter {

    @Bean
    public RouterFunction<?> routeLog(LogHandler logHandler){
        return route()
            .POST("/api/log",logHandler::saveLog)
            .build();
    }
}
~~~
~~~java
@Component
@RequiredArgsConstructor
public class LogHandler {
    private final LogRepository logRepository;

    public Mono<ServerResponse> saveLog(ServerRequest request){
        return request.bodyToMono(SaveLogRequest.class)
            .flatMap(logDto -> {
                final Log log = LogMapper.mapToLog(logDto);
                return logRepository.save(log);
            })
            .flatMap(log -> ServerResponse.ok().build());
    }
}
~~~


### 애노테이션과 함수형 동시 사용

WebFlux에서는 @Controller와 함수형 엔드포인트를 동시에 사용할 수 없습니다. DispatcherHandler에서 HandlerMapping을 찾을때 제일 처음 조건에 충족하는 HandlerMapping 구현체가 사용됩니다. 이때 애노테이션 기반 RequestMappingHandlerMapping과 함수형 엔드포인트 기반 RouterFunctionMapping이 DispatcherHandler 내부 HandlerMapping List에 들어올때 함수형 엔드포인트 기반 RouterFunctionMapping이 RequestMappingHandlerMapping보다 순서가 앞서있습니다. 따라서 동일한 URI를 가지는 HandlerMapping이 있을때 함수형 엔드포인트 HandlerMapping만 사용됩니다.

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerMappingFinder {
    private final ApplicationContext applicationContext;
    private final DispatcherHandler dispatcherHandler;

    @EventListener(ApplicationReadyEvent.class)
    void init(){
        Collection<HandlerMapping> values = applicationContext.getBeansOfType(HandlerMapping.class).values();
        for (HandlerMapping value : values) {
            log.info("handlerMapping: {}",value);
        }

        List<HandlerMapping> handlerMappings = dispatcherHandler.getHandlerMappings();
        for (HandlerMapping handlerMapping : handlerMappings) {
            log.info("DispatcherHandler HandlerMapping: {}", handlerMapping);
        }
    }
}
~~~
~~~
2023-08-06T19:55:58.182+09:00 HandlerMappingFinder      : handlerMapping: result.method.annotation.RequestMappingHandlerMapping@21a5b599
2023-08-06T19:55:58.183+09:00 HandlerMappingFinder      : handlerMapping: function.server.support.RouterFunctionMapping@35e689a0
2023-08-06T19:55:58.183+09:00 HandlerMappingFinder      : handlerMapping: handler.SimpleUrlHandlerMapping@43b6cb1c
2023-08-06T19:55:58.183+09:00 HandlerMappingFinder      : DispatcherHandler HandlerMapping: function.server.support.RouterFunctionMapping@35e689a0
2023-08-06T19:55:58.183+09:00 HandlerMappingFinder      : DispatcherHandler HandlerMapping: result.method.annotation.RequestMappingHandlerMapping@21a5b599
2023-08-06T19:55:58.183+09:00 HandlerMappingFinder      : DispatcherHandler HandlerMapping: handler.SimpleUrlHandlerMapping@43b6cb1c
~~~

> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)