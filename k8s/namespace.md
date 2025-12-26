# Namespace

Namespace는 한 클러스터 안에서 리소스를 논리적으로 나눠서 이름 충돌을 줄이고, 팀/환경 단위로 관리하기 쉽게 만드는 범위(Scope)입니다. 리소스 이름은 같은 Namespace 안에서만 유일하면 되고, 다른 Namespace에서는 같은 이름을 다시 사용할 수 잇습니다.

기본적으로 default, kube-system, kube-public 로 Namespace가 생성됩니다. 

* default: Namespace를 지정하지 않았을 때 기본으로 리소스가 설정되는 Namespace
* kube-system: 쿠버네티스 시스템 컴포넌트용
* kube-public: 모두가 접근할 수 있는 공개 성격의 리소스를 설정

## kube-system

kube-system은 클러스터 자체가 동작하기 위해 필요한 시스템 컴포넌트(컨트롤 플레인/노드 에이전트/애드온)들이 올라가는 공간입니다. 따라서, 여기에 생성되는 요소들은 보통 사용자가 배포한 앱이 아닌, 쿠버네티스와 네트워킹 같은 핵심 기능을 구성하는 Pod, Deployment, Service 등입니다.

* DNS: 클러스터 내부 서비스 디스커버리(ex: my-svc.my.ns.svc.cluster.local)를 제공하는 DNS 서버 Pod/Service
* 네트워킹 플러그인 구성 요소: Calico/Flannel/Cilium 같은 CNI는 보통 노드마다 동작해야하기 때문에, DaemonSet 형태로 kube-system에 설치됩니다. 보통 네트워크 정책/라우팅/터널링 등을 담당합니다.
* kube-proxy: Service(ClusterIP/NodePort 등)의 트래픽을 노드 레벨에서 처리하기 위한 컴포넌트로, 일반적으로 노드마다 떠야해 DaemonSet으로 보입니다.
* 컨트롤 플레인 컴포넌트: kube-apiserver, kube-controller-manager, kube-scheduler, etcd 같은 구성 요소가 pod로 확인됩니다.