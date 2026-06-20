#!/usr/bin/env bash
set -euo pipefail

rm -f /tmp/hbase-ready

stop_hbase() {
  if kill -0 "${hbase_pid}" 2>/dev/null; then
    kill -TERM "${hbase_pid}"
  fi
}

trap stop_hbase TERM INT

hbase master start &
hbase_pid=$!

for attempt in $(seq 1 120); do
  if ! kill -0 "${hbase_pid}" 2>/dev/null; then
    wait "${hbase_pid}"
    exit 1
  fi

  if echo "status 'simple'" | hbase shell -n > /tmp/hbase-status 2>&1 \
      && grep -q "active master" /tmp/hbase-status; then
    break
  fi

  if [[ "${attempt}" -eq 120 ]]; then
    echo "HBase did not become ready in time" >&2
    cat /tmp/hbase-status >&2
    exit 1
  fi

  sleep 2
done

cat <<'HBASE_COMMANDS' | hbase shell -n
create 'travel_flight_prices', {NAME => 'p'} unless exists 'travel_flight_prices'
create 'travel_hotel_prices', {NAME => 'p'} unless exists 'travel_hotel_prices'
HBASE_COMMANDS

touch /tmp/hbase-ready
wait "${hbase_pid}"
