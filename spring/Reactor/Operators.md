# Operators

## Operator 란?

Reactor와 같은 리액티브 프로그래밍은 Operator로 시작하여 Operator로 끝난다고 할 수 있습니다.

Operator는 데이터를 가공하는 역할을 합니다. 이러한 Operator는 Flux나 Mono로 반환함으로써 메서드 체이닝을 구성하여 연속적으로 Operator를 구성할 수 있습니다. 즉, Upstream으로부터 emit된 데이터를 가공하여 다시 Downstream으로 emit하는 역할을 가진다고 할 수 있습니다.

## Operator 목차

> [1. Sequence 생성을 위한 Operator](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/operators/SequenceEmitOperators.md)
> 
> [2. Sequence 필터링 Operator](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/operators/SequenceFilteringOperators.md)
> 
> [3. Sequence 변환 Operator](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/operators/SequenceTransOperators.md)
> 
> [4. Sequence의 내부 동작 확인을 위한 Operator](https://github.com/tlarbals824/TIL/blob/main/spring/Reactor/operators/SequenceDoOnOperators.md)
> 
> [5. 에러 처리를 위한 Operator]()
> 
> [6. Sequence의 동작 시간 측정을 위한 Operator]()
> 
> [7. Flux Sequence 분할을 위한 Operator]()
> 
> [8. 다수의 Subscriber에게 Flux를 멀티캐스팅하기 위한 Operator]()


> 참고 :
>
> 스프링으로 시작하는 리액티브 프로그래밍(https://product.kyobobook.co.kr/detail/S000201399476)