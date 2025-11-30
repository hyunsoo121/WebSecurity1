#!/bin/bash
# LocalStack S3 버킷 자동 생성 스크립트

# 스크립트 실행 권한이 있는지 확인하세요: chmod +x init-localstack.sh

BUCKET_NAME="${APP_S3_BUCKET:-websecurity-images}"

echo "1. LocalStack S3 서비스가 완전히 부팅될 때까지 10초 대기 (안정화 목적)."
sleep 10

echo "2. 버킷 ${BUCKET_NAME} 생성을 시도합니다."

# awslocal s3 mb 명령어로 버킷 생성
awslocal s3 mb s3://"$BUCKET_NAME"

if [ $? -eq 0 ]; then
    echo "✅ S3 bucket ${BUCKET_NAME} successfully created."
else
    # 버킷이 이미 존재하는지 확인
    awslocal s3 ls s3://"$BUCKET_NAME" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ S3 bucket ${BUCKET_NAME} already exists."
    else
        echo "❌ ERROR: S3 bucket ${BUCKET_NAME} creation failed."
        exit 1
    fi
fi