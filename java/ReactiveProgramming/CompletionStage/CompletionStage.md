# CompletionStage

---

## Functional Interface

---

* 함수형 프로그래밍을 지원하기 위해 java 8부터 도입
* 1개의 추상 메서드를 갖고 있는 인터페이스
* 함수를 1급 객체로 사용할 수 있다.
    * 함수를 변수에 할당하거나 인자로 전달하고 반환값으로 사용가능
    * 1급 객체는 다른 객체들에 일반적으로 적용 가능한 연산을 모두 지원하는 객체를 가리킨다
* Function, Consumer, Supplier, Runnable등
* 함수형 인터페이스를 구현한 익명 클래스를 람다식으로 변경 가능

~~~java

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}
~~~

~~~java

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
~~~

~~~java

@FunctionalInterface
public interface Supplier<T> {
    R get();
}
~~~

~~~java

@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
~~~

## CompletionStage 연산자

---

* [thenAccept(Consumer)](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/CompletionStage/CompletionStage/CompletionStageThenAccept.md)
* [thenApply(Function)](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/CompletionStage/CompletionStage/CompletionStageThenApply.md)
* [thenCompose(Function)](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/CompletionStage/CompletionStage/CompletionStageThenCompose.md)
* [thenRun(Runnable)](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/CompletionStage/CompletionStage/CompletionStageThenRun.md)
* [exceptionally(Function)](https://github.com/tlarbals824/TIL/blob/main/java/ReactiveProgramming/CompletionStage/CompletionStage/CompletionStageExceptionally.md)
