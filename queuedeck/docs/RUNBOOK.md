# QueueDeck Runbook: start to finish, with the screenshots to capture

Run everything from the repo root on Linux, macOS or **WSL2** (Ubuntu). 📸 marks a screenshot for the report.

Tools: Git, JDK 17, Maven 3.9, Docker, Ansible (`pip install ansible` gives you `community.docker`), Minikube, kubectl, `jq` (optional).

---

## COMPONENT 1: Git, Maven, Jenkins

### 1. Prove the app works by itself
```bash
mvn clean verify                      # expect: Tests run: 9, Failures: 0 ... BUILD SUCCESS
java -jar target/queuedeck-1.0.0-SNAPSHOT.jar
# in a 2nd terminal:
scripts/smoke-test.sh                 # 📸 201 responses, a 429 when the queue is full, then jobs dequeued in policy order
```

### 2. Git: build real branch/merge history
Create an **empty** GitHub repo called `queuedeck` (no README), then:
```bash
git init -b main
git add pom.xml src .gitignore README.md
git commit -m "feat: QueueDeck job-queue API (FIFO/LIFO/priority) with JUnit tests"
git remote add origin https://github.com/<you>/queuedeck.git
git push -u origin main

git checkout -b feature/ci-pipeline
git add Jenkinsfile .github jenkins
git commit -m "ci: Jenkins pipeline (test stage, versioned artifact) + GitHub Actions"
git push -u origin feature/ci-pipeline
```
On GitHub, open a **Pull Request** feature/ci-pipeline → main and **merge** it. Then:
```bash
git checkout main && git pull
git checkout -b feature/deploy
git add Dockerfile .dockerignore ansible k8s scripts docs
git commit -m "deploy: Ansible role, multi-stage Dockerfile, k8s manifests with probes/ConfigMap"
git push -u origin feature/deploy        # PR -> merge again
git checkout main && git pull
git log --oneline --graph --all          # 📸 branches + merge commits
```
Also add the course coordinator as a collaborator (Settings → Collaborators) and put the URL in `repo_link.txt`.

### 3. Jenkins
```bash
docker compose -f jenkins/docker-compose.yml up -d --build
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
1. Open http://localhost:8090, install the **suggested plugins**, create an admin user.
2. **Manage Jenkins → Tools → Maven installations → Add**, name it **`Maven3`**, tick *Install automatically*. Save.
3. **New Item → `queuedeck` → Pipeline**.
   - *Build Triggers*: tick **Poll SCM**, schedule `* * * * *` (the Jenkinsfile also sets this after the first run).
   - *Pipeline*: **Pipeline script from SCM** → Git → your repo URL → branch `*/main` → Script Path `Jenkinsfile`.
     (Private repo? Add a GitHub username + personal-access-token credential here.)
   - 📸 the config page (SCM + trigger + script path).
4. Click **Build Now** once. The first build registers the poll trigger.
5. **Prove CI is automatic**: make a small change and push it, e.g.
   ```bash
   echo "" >> README.md && git commit -am "docs: trigger CI" && git push
   ```
   Within about a minute a new build starts by itself, labelled "Started by an SCM change". 📸 that.
6. 📸 **Console Output** of a green build (Test → Build → Docker Image stages, `BUILD SUCCESS`).
7. 📸 the build page showing **Build Artifacts: `queuedeck-1.0.N-<sha>.jar`** and the **Test Result** (9 passed).
8. 📸 the **Stage View**, which shows the Test stage running before Build.

### 4. CI enhancement evidence
- **Tests in CI**: Test Result page (9/9) and the Test stage in Stage View. 📸
- **Versioning**: artifact name `queuedeck-1.0.<BUILD_NUMBER>-<git sha>.jar`, build name `#N 1.0.N-sha`, and
  `docker images queuedeck` showing the same tag. 📸 Check the sha matches `git log -1 --format=%h`.
- **GitHub Actions**: repo → *Actions* tab → green "CI" run with the uploaded artifact. 📸
- Optional "failing test" demo: break an assertion, push, and show the pipeline go **red at Test** with Build skipped, then revert.

---

## COMPONENT 2: Ansible, Docker, Kubernetes

### 5. Ansible
```bash
mvn clean package                                   # the role deploys target/queuedeck-*.jar
docker build -t queuedeck-target ansible/target
docker run -d --name queuedeck-target --privileged --cgroupns=host \
  -v /sys/fs/cgroup:/sys/fs/cgroup:rw -p 8081:8080 queuedeck-target

cd ansible
ansible-galaxy collection install community.docker  # skip if you installed the full "ansible" package
ansible-playbook -i inventory.ini playbook.yml      # 📸 PLAY RECAP: failed=0, plus the "Report" line
ansible-playbook -i inventory.ini playbook.yml      # 📸 2nd run: changed=0, handler NOT triggered (idempotent)
cd ..
curl -s localhost:8081/ | jq                        # 📸 policy PRIORITY, capacity 10 (from group_vars)
```
**Handler demo**: run `ansible-playbook -i inventory.ini playbook.yml -e queue_capacity=20` and show
`RUNNING HANDLER [queuedeck : Restart queuedeck]` fire because the env file changed. 📸

Role structure for the report: `tree ansible` 📸. The role was scaffolded with `ansible-galaxy init roles/queuedeck`.
If your examiner wants to see you run that command, run it in a scratch folder and screenshot it.

> If the systemd container won't boot on your machine (some Docker Desktop setups), use any Ubuntu VM
> (Multipass/VirtualBox) and the SSH line in `inventory.ini` instead. The playbook doesn't change.

### 6. Docker (multi-stage)
```bash
docker build -t queuedeck:local .
docker images queuedeck                               # 📸 note the runtime image is JRE-only
docker run --rm -d -p 8080:8080 --name cl queuedeck:local && sleep 8 && scripts/smoke-test.sh && docker stop cl
```
For the multi-stage enhancement, 📸 the build log showing `[build 5/5]` and the final stage, and compare sizes:
`docker images maven:3.9-eclipse-temurin-17` (build tools) vs `queuedeck:local`.

### 7. Kubernetes
```bash
minikube start --memory=4096 --cpus=2
eval $(minikube docker-env)              # build straight into Minikube's Docker, no registry needed
docker build -t queuedeck:v1 .

kubectl apply -f k8s/
kubectl rollout status deployment/queuedeck
kubectl get pods -o wide                 # 📸 3/3 Running
kubectl get svc queuedeck                # 📸 NodePort 30080
URL=$(minikube service queuedeck --url)
scripts/smoke-test.sh "$URL"             # 📸 app responding through the Service
scripts/spread.sh "$URL" 30              # 📸 requests spread across 3 pod names
```
(On Docker-driver Minikube on Windows/macOS, `minikube service queuedeck --url` keeps a tunnel open. Run it in its
own terminal and use the URL it prints.)

### 8. Scaling
```bash
scripts/scale-demo.sh                    # 📸 3 pods, then 5, then 3
# or by hand: kubectl scale deployment queuedeck --replicas=5 ; kubectl get pods -w
scripts/spread.sh "$URL" 40              # 📸 (while at 5) traffic across 5 pods
```

### 9. Deployment enhancements: evidence
**Rollback (main demo):**
```bash
scripts/rollback-demo.sh
```
📸 in order: the new pod in `CrashLoopBackOff`, its log line `queuedeck.capacity ... must be greater than or equal to 1`,
`curl` still answering from the **old** pods (zero downtime thanks to `maxUnavailable: 0` + readiness probe),
`rollout undo`, then 3/3 Running and `kubectl rollout history`.

**ConfigMap:**
```bash
kubectl get configmap queuedeck-config -o yaml        # 📸
kubectl patch configmap queuedeck-config -p '{"data":{"QUEUE_POLICY":"PRIORITY"}}'
kubectl rollout restart deployment/queuedeck && kubectl rollout status deployment/queuedeck
curl -s "$URL/" | jq .policy                           # 📸 "PRIORITY", with no image rebuild
```
**Probes:** `kubectl describe pod <pod> | grep -A3 -E "Liveness|Readiness|Startup"` 📸

**Metrics:** `curl -s "$URL/actuator/prometheus" | grep queuedeck_` 📸. For the report: Prometheus would scrape this
endpoint every 15s and track `rate(queuedeck_rejected_total[1m]) / (rate(queuedeck_enqueued_total[1m]) + rate(queuedeck_rejected_total[1m]))`
as the live rejection ratio, alert on a sustained non-zero value, and graph `queuedeck_depth` for backlog.

**Versioned image from Jenkins into Kubernetes (ties both components together):**
```bash
eval $(minikube docker-env); docker build --build-arg REVISION=1.0.7-abc1234 -t queuedeck:1.0.7-abc1234 .
kubectl set image deployment/queuedeck queuedeck=queuedeck:1.0.7-abc1234
kubectl annotate deployment/queuedeck kubernetes.io/change-cause="1.0.7-abc1234 from Jenkins build #7" --overwrite
curl -s "$URL/" | jq .version             # 📸 shows 1.0.7-abc1234
```

Optional bonus (HPA): `minikube addons enable metrics-server && kubectl apply -f k8s/extras/hpa.yaml && kubectl get hpa`.

---

## Clean-up
```bash
kubectl delete -f k8s/ ; minikube stop
docker rm -f queuedeck-target
docker compose -f jenkins/docker-compose.yml down
```

## Resume lines
**QueueDeck: CI/CD Pipeline & Kubernetes Deployment** | github.com/<you>/queuedeck
*Java 17, Spring Boot, Maven, Git, Jenkins, GitHub Actions, Docker, Ansible, Kubernetes*
- Built a bounded job-queue simulator REST API (FIFO, LIFO, priority; depth/enqueue/reject metrics, 429 backpressure) with a Jenkins pipeline triggered on every Git push that runs JUnit tests before packaging.
- Versioned every artifact and Docker image by build number and commit SHA, and mirrored the pipeline in GitHub Actions.
- Automated server provisioning with an Ansible role (variables, templated systemd unit, change-triggered restart handler).
- Deployed on Kubernetes with 3→5 replicas behind a Service, ConfigMap-driven config, and startup/liveness/readiness probes, using a multi-stage Docker image.
- Demonstrated a zero-downtime failed rollout and recovery with `kubectl rollout undo`.

## Viva prep: likely questions
- *Why poll SCM instead of a webhook?* GitHub can't reach a laptop's localhost Jenkins without a tunnel such as ngrok. Polling every minute needs no public URL.
- *What does the handler buy you?* Idempotency. The second run reports `changed=0` and doesn't restart the service.
- *Why does the bad rollout cause no downtime?* `maxUnavailable: 0` keeps old pods until a new pod is Ready, and the readiness probe never passes on the broken pod.
- *Why can a job enqueued through one pod not be dequeued through another?* Each pod has its own in-memory queue, and a restart loses it. The fix is a shared broker (Redis, RabbitMQ or SQS).
- *Liveness vs readiness vs startup?* Restart if dead / stop sending traffic if not ready / don't judge liveness until boot finishes.
