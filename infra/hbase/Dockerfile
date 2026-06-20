FROM eclipse-temurin:11-jre-jammy

ARG HBASE_VERSION=2.5.8

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl procps \
    && curl --fail --location --retry 3 \
        "https://archive.apache.org/dist/hbase/${HBASE_VERSION}/hbase-${HBASE_VERSION}-bin.tar.gz" \
        --output /tmp/hbase.tar.gz \
    && mkdir -p /opt/hbase \
    && tar --extract --gzip --file /tmp/hbase.tar.gz \
        --directory /opt/hbase --strip-components=1 \
    && rm /tmp/hbase.tar.gz \
    && apt-get purge --yes --auto-remove curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /data/hbase /data/zookeeper

ENV HBASE_HOME=/opt/hbase
ENV PATH="${HBASE_HOME}/bin:${PATH}"
ENV HBASE_HEAPSIZE=1024

COPY infra/hbase/hbase-site.xml /opt/hbase/conf/hbase-site.xml
COPY infra/hbase/entrypoint.sh /usr/local/bin/hbase-entrypoint

RUN sed -i 's/\r$//' /usr/local/bin/hbase-entrypoint \
    && chmod +x /usr/local/bin/hbase-entrypoint

EXPOSE 2181 16000 16010 16020 16030

ENTRYPOINT ["/usr/local/bin/hbase-entrypoint"]
