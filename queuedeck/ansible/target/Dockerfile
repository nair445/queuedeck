# Target "server" for the Ansible demo: Ubuntu 22.04 running systemd inside a container.
# Ansible reaches it through the Docker API (see ansible/inventory.ini), so no SSH is needed.
#   docker build -t queuedeck-target ansible/target
#   docker run -d --name queuedeck-target --privileged --cgroupns=host \
#     -v /sys/fs/cgroup:/sys/fs/cgroup:rw -p 8081:8080 queuedeck-target
FROM ubuntu:22.04
ENV container=docker DEBIAN_FRONTEND=noninteractive
# python3-apt is required by Ansible's apt module
RUN apt-get update \
 && apt-get install -y --no-install-recommends systemd systemd-sysv python3 python3-apt curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*
STOPSIGNAL SIGRTMIN+3
VOLUME ["/sys/fs/cgroup"]
CMD ["/sbin/init"]
