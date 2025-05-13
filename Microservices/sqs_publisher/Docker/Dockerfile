FROM python:3.9-alpine

WORKDIR /sqs_publisher

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

RUN pip install pytest

COPY . .

EXPOSE 15000

ENV AWS_ACCESS_KEY=
ENV AWS_SECRET_KEY=
ENV AWS_REGION=
ENV SQS_QUEUE_URL=
ENV PYTHONUNBUFFERED=1

CMD [ "python", "sqs_publisher.py" ]

