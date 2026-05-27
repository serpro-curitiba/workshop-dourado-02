#!/usr/bin/env bash
set -e
cd /mnt/c/Users/s006420399/Desenvolvimento/curso_microsoft/workshop-dourado-02/infra
echo "=== terraform fmt check ==="
terraform fmt -check -recursive && echo "fmt OK" || terraform fmt -recursive && echo "fmt applied"
echo "=== terraform init (sem backend) ==="
terraform init -backend=false -input=false
echo "=== terraform validate ==="
terraform validate
echo "=== TERRAFORM OK ==="
