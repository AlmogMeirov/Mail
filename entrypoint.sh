#!/bin/bash

IP=$(hostname -I | awk '{print $1}')
PORT="$2"

echo "SERVER_IP=$IP" > /app/data/ip.env
echo "SERVER_PORT=$PORT" >> /app/data/ip.env

exec ./build/MailProject "$@"
