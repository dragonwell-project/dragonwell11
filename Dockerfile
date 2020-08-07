FROM alpine:3.4

RUN echo -e "https://mirrors.ustc.edu.cn/alpine/v3.4/main\nhttps://mirrors.ustc.edu.cn/alpine/v3.4/community" > /etc/apk/repositories \
    && apk update \
    && apk --no-cache add 'g++<6' \
    && apk --no-cache add autoconf bash gawk grep make zip file alsa-lib-dev cups-dev mercurial tar fontconfig-dev freetype-dev giflib-dev lcms2-dev libelf-dev libffi-dev libjpeg-turbo-dev libx11-dev libxext-dev libxrandr-dev libxrender-dev libxt-dev libxtst-dev linux-headers zlib-dev \
    && apk --no-cache add openjdk11 --repository=https://mirrors.ustc.edu.cn/alpine/edge/community


