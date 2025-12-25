# Service

Service는 Pod들이 교체되거나 IP가 바뀌어도 고정된 접점으로 트래픽을 받을 수 있게 해주는 네트워크 오브젝트이며, Service를 기준으로 내부/외부 접근 경로를 안정화합니다.

Service는 `kubectl expose`를 통해 생성할 수 있으며, type 옵션을 통해 Service의 타입을 지정할 수 있습니다.

## Service 타입

Service 타입으로는 ClusterIP, NodePort, LoadBalancer, ExternalName로 총 4개가 존재합니다.

* ClusterIP는 클러스터 내부에서만 접근 가느한 가상 IP를 제공합니다. 이는 특정 마이크로서비스 내부에서 API서버와 데이터베이스 접근과 통신에서 사용됩니다.
* NodePort는 노드의 특정 포트를 외부로 열어 NodeIP:NodePort로 요청을 외부에서 보낼 수 있도록 합니다.
* LoadBalancer는 외부 로드밸런서를 통해 외부에 노출하고 일반적으로 외부에서 접근 가능한 IP를 제공합니다. 외부 트래픽을 받는 서비스에서 주로 사용됩니다.
* ExternalName은 외부 DNS 이름에 매핑해 클러스터 DNS가 CNAME을 반환하도록 하는 특수 타입입니다.