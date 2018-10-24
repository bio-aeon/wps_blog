FROM ubuntu:16.04

ARG RUN_UID=1000

ENV DJANGO_CONFIGURATION "Prod"

RUN apt-get update && \
apt-get upgrade -y &&  \
apt-get install curl git wget unzip nano net-tools python-virtualenv supervisor nginx \
build-essential python-dev libmysqlclient-dev zlib1g-dev libjpeg-turbo8-dev libtidy-dev -y && \
useradd -u ${RUN_UID} -m user && \
mkdir /home/user/project

COPY ./ /home/user/project/

RUN chown -R ${RUN_UID}:${RUN_UID} /home/user/project

RUN cd /home/user/project \
    && cp sys/supervisord.conf /etc/supervisor/conf.d/ \
    && cp sys/app.conf /etc/supervisor/conf.d/ \
    && cp sys/nginx.conf /etc/nginx/sites-enabled/default

USER user

RUN cd /home/user/project \
    && virtualenv --no-site-packages --python=/usr/bin/python2.7 ../venv \
    && ../venv/bin/pip install -r requirements.txt \
    && mkdir ../logs \
    && mkdir ../pids

USER root

WORKDIR /home/user/project

CMD service nginx restart && /usr/bin/supervisord
