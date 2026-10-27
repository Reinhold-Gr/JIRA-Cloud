# Merksatz

Lambda = Dein Programm

API Gateway = Die öffentliche Eingangstür

Terraform = Die Bauanleitung, mit der AWS diese Komponenten automatisch erstellt

Für dein Jira-PDF-Projekt würde ich erwarten, dass ein Cloud- oder DevOps-Team später eine Terraform-Konfiguration bereitstellt, damit Lambda, API Gateway, DynamoDB und S3 nicht von Hand konfiguriert werden müssen.



## Für das Jira-PDF-Projekt könnte eine einfache Terraform-Konfiguration etwa so aussehen:

Ziel

# Terraform soll automatisch erzeugen:

Plain Text
AWS
├── Lambda-Funktion
├── API Gateway
├── DynamoDB
└── S3-Archiv

1. Provider konfigurieren

### provider.tf

Terraform
terraform {
required_version = ">= 1.6"
 
required_providers {
aws = {
source = "hashicorp/aws"
version = "~> 5.0"
}
}
}
 
provider "aws" {
region = "eu-central-1"
}

Hier wird festgelegt:

Terraform-Version
AWS Provider
Region Frankfurt (eu-central-1)


2. Lambda-Funktion

### lambda.tf

Terraform
resource "aws_lambda_function" "pdf_orchestrator" {
function_name = "pdf-orchestrator"
 
filename = "lambda.zip"
source_code_hash = filebase64sha256("lambda.zip")
 
role = aws_iam_role.lambda_role.arn
handler = "lambda_function.lambda_handler"
runtime = "python3.12"
 
timeout = 30
 
environment {
variables = {
JIRA_URL = "https://firma.atlassian.net"
}
}
}
 

Terraform erstellt dann automatisch:

Plain Text
Lambda:
pdf-orchestrator
 
3. IAM-Rolle für Lambda

### iam.tf

Terraform
resource "aws_iam_role" "lambda_role" {
name = "pdf-orchestrator-role"
 
assume_role_policy = jsonencode({
Version = "2012-10-17"
Statement = [
{
Action = "sts:AssumeRole"
Effect = "Allow"
Principal = {
Service = "lambda.amazonaws.com"
}
}
]
})
}

Diese Rolle erlaubt AWS, die Lambda-Funktion auszuführen.

4. DynamoDB für Idempotenz

### dynamodb.tf

Terraform
resource "aws_dynamodb_table" "idempotency" {
name = "pdf-idempotency"
billing_mode = "PAY_PER_REQUEST"
 
hash_key = "CorrelationId"
 
attribute {
name = "CorrelationId"
type = "S"
}
}

In dieser Tabelle könntest du speichern:

Plain Text
ABC-123_invoice_v1
ABC-456_invoice_v1
ABC-789_invoice_v2

und so Doppelerzeugungen verhindern.

5. S3-Archiv

### S3.tf

Terraform
resource "aws_s3_bucket" "archive" {
bucket = "jira-pdf-archive"
}

Darin landen später:

Plain Text
invoice-2026-1001.pdf
invoice-2026-1002.pdf
report-ABC-123.pdf


6. API Gateway

### api.tf

Terraform
resource "aws_apigatewayv2_api" "pdf_api" {
name = "pdf-api"
protocol_type = "HTTP"
}

Terraform erzeugt dann automatisch eine URL wie:

Plain Text
https://abc123.execute-api.eu-central-1.amazonaws.com
Verzeichnisstruktur

Typischerweise sieht das Projekt so aus:

Plain Text
terraform/
├── provider.tf
├── lambda.tf
├── iam.tf
├── dynamodb.tf
├── s3.tf
├── api.tf
└── variables.tf
 
src/
└── lambda_function.py
Deployment

Terraform wird anschließend mit wenigen Befehlen ausgeführt:

Shell
terraform init

Provider herunterladen.

Shell
terraform plan

Anzeigen, was erstellt wird.

Shell
terraform apply

Ressourcen tatsächlich anlegen.

Warum Terraform in deinem Fall interessant wäre

Wenn ihr später drei Umgebungen habt:

Plain Text
DEV
TEST
PROD

muss niemand mehr im AWS-Portal herumklicken.

Stattdessen:

Shell
terraform apply

und Terraform erzeugt identische:

Lambda-Funktionen
API-Gateways
DynamoDB-Tabellen
S3-Buckets

für jede Umgebung.

Für ein Team mit DevOps- oder Cloud-Betrieb ist das heute der Standardansatz.  
Für deinen PoC würde ich jedoch zunächst die AWS-Komponenten manuell anlegen  
und Terraform erst einführen, wenn die Architektur fachlich steht.