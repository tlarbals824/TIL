# Redis pub/sub

* Redis는 아주 가벼운 pub/sub 기능을 제공합니다.
* Redis 노드에 집근할 수 있는 모든 클라이언트는 발행자와 구독자가 될 수 있습니다.
* 발행자는 특정 채널에 메시지를 보낼 수 있으며, 구독자는 특정 채널을 듣고있다가 메시지를 읽어갈 수 있습니다.
* Redis의 pub/sub은 최소한의 메시지 전달 기능만 제공합니다. 
* 발행자는 메시지를 채널로 보낼 수 있을 뿐, 어떤 구독자가 메시지를 읽어가는지, 정상적으로 모든 구독자에게 메시지가 전달됐는지 확인할 수 없습니다.
* 구독자는 메시지를 받을 수 있지만 해당 메시지가 언제 어떤 발행자에 의해 생성됐는지 등의 메타데이터는 알 수 없습니다.
* 한 번 전파된 데이터는 레디스에 저장되지 않습니다. 이로인해 정합성이 중요한 데이터를 전달하기에는 적합하지 않을 수 있습니다.

## 메시지 publish 하기

* Redis에서는 PUBLISH 명령어를 통해 메시지를 발행할 수 있습니다.

~~~
> PUBLISH <channel> <message>
(integer) <number>
~~~

* channel은 메시지를 발행할 채널을 의미하며, message는 발행할 메시지를 의미합니다.
* number는 메시지를 받는 구독자의 수를 의미합니다.

## 메시지 subscribe 하기

* Redis에서는 SUBSCRIBE 명령어를 통해 메시지를 구독할 수 있습니다.

~~~
> SUBSCRIBE <channel> [<channel> ...]
1) "subscribe"
2) "<channel>"
3) (integer) <number>
1) "message"
2) "<channel>"
3) "<message>"
~~~

* channel은 메시지를 구독할 채널을 의미합니다. 여러 채널을 구독할 수 있습니다.
* number는 구독한 채널 순서를 의미합니다.
* message는 발행된 메시지를 의미합니다.
* 구독자가 수행할 수 있는 커맨드는 subscribe, ssubscribe, sunsubscribe, psubscribe, unsubscribe, punsubscribe, ping, reset, quit이 있습니다.