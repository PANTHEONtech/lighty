# Kubernetes clustered lighty.io NETCONF/RESTCONF application

This example application demonstrates clustered lighty.io application deployed in Kubernetes environment.

Components used:
* Lighty Controller
* NETCONF south-bound plugin
* RESTCONF north-bound plugin
* OpenDaylight Swagger servlet

## Application architecture
![Application Architecture](docs/app-k8s-deployment.svg)

# Kubernetes deployment
Follow this guide in order to deploy this lighty.io cluster demo into kubernetes cluster.
This demo was tested using kubernetes cluster v1.19.2.

## Run demo in local environment using microk8s
To start cluster:
1. Install [microk8s](https://microk8s.io/docs) tool
2. Build lighty.io cluster example application:  
  `mvn clean install -pl :lighty-cluster-app -am`
3. Build Docker Image:  
  `docker build . -f Dockerfile.k8s -t lighty-k8s-cluster:1.0.0-SNAPSHOT`
4. Save Docker Image as .tar:  
  `docker save --output="target/lighty-k8s-cluster:1.0.0-SNAPSHOT.tar" lighty-k8s-cluster:1.0.0-SNAPSHOT`
5. Copy saved image into microk8s environment (more information [here](https://microk8s.io/docs/registry-images)).  
  NOTE: defining namespace (`-n k8s.io`) is needed for versions prior to 1.17.  
  `microk8s ctr --namespace k8s.io image import target/lighty-k8s-cluster\:1.0.0-SNAPSHOT.tar`
6. Enable DNS and Ingress:  
  `microk8s enable dns ingress`
7. Apply configurations:  
  `microk8s kubectl apply -f lighty-k8s-cluster-roles.yaml`  
  `microk8s kubectl apply -f lighty-k8s-cluster-deployment.yaml`

To execute REST requests on lighty.io app:
- Inside of the cluster RESTCONF is exposed via port 8888
- Inside of the cluster `akka` management is exposed via port 8558
- Kubernetes `Ingress` contains configuration to redirect requests issued on certain hosts to these services
    - Host `management.lighty.io` is redirected to management port
    - Host `restconf.lighty.io` is redirected to RESTCONF port
- When executing REST request, either:
    - add entries for both hosts to the `/etc/hosts` file (pointing to the `127.0.0.1`) and in the request use URL
    `your-host.com/rest/of/the/url`
    - or in the request use URL `127.0.0.1:80/rest/of/the/url` and set `Host` header manually 
- To see examples of REST request check Postman collection `lighty_cluster.postman_collection`.

To access Dashboard:
1. Enable Dashboard:  
  `microk8s enable dashboard`
2. Obtain token to log in:  
  `token=$(microk8s kubectl -n kube-system get secret | grep default-token | cut -d " " -f1)`  
   `microk8s kubectl -n kube-system describe secret $token`
3. You also may need to enable port forwarding for dashboard:  
    `microk8s kubectl port-forward -n kube-system service/kubernetes-dashboard 10443:443`
4. Go to URL `https://127.0.0.1:10443`
5. Log in using obtained token in previous steps

To stop lighty.io cluster example and clean microk8s environment:
- Delete started components:  
  `microk8s kubectl delete -f lighty-k8s-cluster-deployment.yaml`  
  `microk8s kubectl delete -f lighty-k8s-cluster-roles.yaml`  
- Reset microk8s:  
  `microk8s reset`
  
To scale up and down:
- Scale to X replications - replace X by desired number (minimum number is 3):  
  `kubectl scale deployment lighty-k8s-cluster --replicas=X`


### Configure pod-restart-timeout
It is used in situation when Cluster member becomes unreachable but his Pod still remains in Kubernetes.
This means there might be just some temporary connection issue.
The cluster healing mechanism will use this timeout to wait for the member giving it a chance to become reachable again
before issuing restart request to Kubernetes.

If not configured, default timeout (30s) will be used.
```
akka.lighty-kubernetes {
    pod-restart-timeout = 60
}
```
