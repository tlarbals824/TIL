# Deployment

Deployment는 쿠버네티스가 특정 상태를 맞추도록 자동으로 관리해주는 워크로드 리소스입니다.
Deployment는 내부적으로 ReplicaSet을 통해 Pod 복제본 수를 유지하고, 업데이트 시 새 ReplicaSet을 만들어 점진적으로 교체하는 방식으로 동작합니다.

## Deployment가 하는 일

* 원하는 Pod 수를 유지합니다. 즉, Pod가 죽으면 설정된 Pod 수에 맞춰 다시 생성합니다.
* 컨테이너 이미지/환경변수/리소스 제한 같은 템플릿 변경을 안전하게 반영합니다.
* 업데이트 과정에서 기존 ReplicaSet과 새로운 ReplicaSet을 함께 관리하면서, 점진적 교체를 수행합니다.