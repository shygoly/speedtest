FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY server.py ./server.py

# WebSocket TCP port and UDP port used by server.py
EXPOSE 10001/tcp
EXPOSE 10002/udp

CMD ["python", "server.py"]
