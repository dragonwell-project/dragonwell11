FROM alibaba-cloud-linux-3-registry.cn-hangzhou.cr.aliyuncs.com/alinux3/alinux3:latest
ADD jdk.tar.gz /
RUN setcap 'cap_checkpoint_restore+eip cap_setpcap+eip' /jdk/lib/criu
RUN groupadd -g 1000 app && useradd -u 1000 -g app -s /bin/sh  -d /home/app -m app
USER app:app
ENV JAVA_HOME=/jdk
ADD classes.tar.gz takepid.sh runapp.sh /home/app
CMD /home/app/runapp.sh