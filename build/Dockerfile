FROM elasticsearch:7.6.2

COPY target/repository-bos-7.6.2.zip /usr/share/elasticsearch/
COPY build/entrypoint.sh /usr/share/elasticsearch/

RUN sh -c 'chown -R 1000 /usr/share/elasticsearch/repository-bos-7.6.2.zip'

RUN chmod a+x /usr/share/elasticsearch/entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]