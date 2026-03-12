FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends libpq-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY blog_admin/ .

RUN python manage.py collectstatic --noinput

RUN useradd -r -s /usr/sbin/nologin appuser
USER appuser

EXPOSE 8000

CMD ["gunicorn", "blog_admin.wsgi:application", "--bind", "0.0.0.0:8000", "--workers", "2"]
