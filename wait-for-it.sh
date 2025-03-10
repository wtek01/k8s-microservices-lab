# À placer dans C:\Dev\k8s-microservices-lab\wait-for-it.sh
#!/bin/sh
# wait-for-it.sh

set -e

host="$1"
port="$2"
shift 2
cmd="$@"

until nc -z "$host" "$port"; do
  >&2 echo "Service on $host:$port is unavailable - sleeping"
  sleep 1
done

>&2 echo "Service on $host:$port is up - executing command"
exec $cmd