# 3장 중단은 어떻게 작동할까?

중단함수는 코틀린 코루틴의 핵심입니다.

코루틴을 중단한다는 것은 실행을 중간에 멈추는 것을 의미합니다. 중단하는 과정에서 코루틴은 Continuation 객체를 반환합니다.
이 객체를 통해서 코루틴을 다시 시작할 수 있습니다.

이러한 코루틴의 특징은 스레드와 많이 다르다는것을 알 수 있습니다. 즉, 스레드는 저장이 불가능하고 멈추는 것만 가능하며 코루틴은 현재 상태에 대해 저장이 가능하며 다시 시작할 수 있습니다.

## 재개

중단 함수는 코루틴을 중단할 수 있는 함수를 말합니다. 이는 중단 함수가 반드시 코루틴에 의해 호출되어야 함을 짐작할 수 있습니다. 즉, 코루틴을 시작하기 위해서는 suspend 
키워드나 runBlocking을 통해 시작해야 합니다.

간단한 예를 통해 코루틴을 멈추고 다시 시작하는 과정에 대해서 살펴보겠습니다.

우선 suspend main함수를 정의하겠습니다.

```Kotlin
suspend fun main() {
    println("Before suspend")

    println("After suspend")
}
```

위 코드를 실행하면 "Before suspend", "After suspend"가 출력됩니다. 그러면 "After suspend"가 출력되기 전에 코루틴을 멈춰볼까요?

코루틴을 멈추기 위한 방법중 가장 간단한 방법은 `suspendCoroutine`을 사용하는 것입니다.

```Kotlin
suspend fun main() {
    println("Before suspend")

    suspendCoroutine<Unit> { continuation ->
        println("In suspendCoroutine")
    }

    println("After suspend")
}

/**
* Before suspend
* In suspendCoroutine
* ...
*/
```

결과를 보면 "In suspendCoroutine"을 출력한 다음 "After suspend"를 출력하지 않았습니다. 즉, 코루틴이 중단되었으며 다시 시작되지 않았다고 볼 수 있습니다.
그렇다면 앞서 설명한 `continuation`은 어디에서 볼 수 있을까요?

`suspendCoroutine` 함수에서 람다 포현식이 실행될 때 `continuation`객체를 인자로 받을 수 있습니다.

```Kotlin
suspend fun main() {
    println("Before suspend")

    suspendCoroutine<Unit> { continuation ->
        println("In suspendCoroutine")
    }

    println("After suspend")
}
```

`continuation` 객체는 `suspendCoroutine` 함수를 호출하고 코루틴이 중단되기 전에 생성되며 사용할 수 있습니다. 이 객체는 이후 저장한 뒤 코루틴을 다시 실행할
시점을 결정하기 위해 사용됩니다.

간단한 예로 코루틴을 중단한 뒤 바로 시작하는 코드를 작성해보겠습니다.

```Kotlin
suspend fun main() {
    println("Before suspend")

    suspendCoroutine<Unit> { continuation ->
        println("In suspendCoroutine")
        continuation.resume(Unit)
    }

    println("After suspend")
}

/**
* Before suspend
* In suspendCoroutine
* After suspend
*/
```

위 예제가 종료된 이유는 `continuation.resume(Unit)`을 통해 코루틴을 다시 시작했기 때문입니다.

그러면 `suspendCoroutine`에서 제네릭으로 `Unit`을 사용한 이유는 무엇일까요?

그 이유는 `suspendCoroutine`에 정의된 제네릭은 코루틴이 중단되고 다시 시작될 때 반환될 값을 정의하고 있기 때문입니다. 위 예제에서는 반환할 값이 없으니 `Unit`을 사용한 것이죠!

이와 비슷하게, `resumeWithException`을 통해 코루틴의 결괏값으로 예외를 전달할 수 있습니다.

## 함수가 아닌 코루틴을 중단시키다.

간단하게 코루틴에 대해서 알아봤는데요, 결국 중단 함수는 코루틴이 아니고, 단지 코루틴을 중단할 수 있는 함수라는 점을 기억해야 합니다.


