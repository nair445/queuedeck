# queuedeck role

Scaffolded with `ansible-galaxy init roles/queuedeck`.

Installs OpenJDK 17, copies the QueueDeck JAR built by Maven/Jenkins, writes an environment file
(port, queue policy, capacity) and a systemd unit, then starts the service and health-checks it.

| Variable | Default | Purpose |
|---|---|---|
| `app_port` | 8080 | Port the API listens on |
| `queue_policy` | FIFO | `FIFO`, `LIFO` or `PRIORITY` |
| `queue_capacity` | 5 | Max jobs waiting before new ones are rejected |
| `artifact_src` | `../target/queuedeck-*.jar` | JAR to deploy |

Handler `Restart queuedeck` fires only when the JAR, env file or unit file changed.
