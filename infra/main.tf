# ─────────────────────────────────────────────────────────────────────────────
# SIFAP 2.0 — Infraestrutura Azure (Terraform)
# ADR-001: Modular Monolith em Azure Container Apps
# ADR-003: tags obrigatórias em todos os recursos
# ─────────────────────────────────────────────────────────────────────────────

terraform {
  required_version = ">= 1.9.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.117"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
  # backend "azurerm" { } — configurar via -backend-config em CI
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = false
      recover_soft_deleted_key_vaults = true
    }
  }
}

# ─── Locals ──────────────────────────────────────────────────────────────────
locals {
  project     = "sifap"
  environment = var.environment
  owner       = var.owner
  location    = var.location

  common_tags = {
    project     = local.project
    environment = local.environment
    owner       = local.owner
    managed_by  = "terraform"
  }

  name_prefix = "${local.project}-${local.environment}"
}

# ─── Resource Group ──────────────────────────────────────────────────────────
resource "azurerm_resource_group" "main" {
  name     = "rg-${local.name_prefix}"
  location = local.location
  tags     = local.common_tags
}

# ─── Log Analytics (monitoramento) ───────────────────────────────────────────
resource "azurerm_log_analytics_workspace" "main" {
  name                = "log-${local.name_prefix}"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  tags                = local.common_tags
}

# ─── Container Apps Environment ──────────────────────────────────────────────
resource "azurerm_container_app_environment" "main" {
  name                       = "cae-${local.name_prefix}"
  location                   = azurerm_resource_group.main.location
  resource_group_name        = azurerm_resource_group.main.name
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  tags                       = local.common_tags
}

# ─── PostgreSQL Flexible Server ──────────────────────────────────────────────
resource "random_password" "pg" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "azurerm_postgresql_flexible_server" "main" {
  name                   = "psql-${local.name_prefix}"
  resource_group_name    = azurerm_resource_group.main.name
  location               = azurerm_resource_group.main.location
  version                = "16"
  administrator_login    = "sifapadmin"
  administrator_password = random_password.pg.result
  storage_mb             = 32768
  sku_name               = "B_Standard_B1ms"
  backup_retention_days  = 7
  tags                   = local.common_tags

  lifecycle {
    ignore_changes = [administrator_password, zone, high_availability]
  }
}

resource "azurerm_postgresql_flexible_server_database" "sifap" {
  name      = "sifap"
  server_id = azurerm_postgresql_flexible_server.main.id
  collation = "pt_BR.utf8"
  charset   = "UTF8"
}

# ─── Key Vault (secrets — nunca em locals/variables) ─────────────────────────
data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                       = "kv-${local.name_prefix}"
  location                   = azurerm_resource_group.main.location
  resource_group_name        = azurerm_resource_group.main.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  purge_protection_enabled   = false
  soft_delete_retention_days = 7
  tags                       = local.common_tags

  access_policy {
    tenant_id          = data.azurerm_client_config.current.tenant_id
    object_id          = data.azurerm_client_config.current.object_id
    secret_permissions = ["Get", "List", "Set", "Delete", "Purge"]
  }
}

resource "azurerm_key_vault_secret" "pg_password" {
  name         = "pg-admin-password"
  value        = random_password.pg.result
  key_vault_id = azurerm_key_vault.main.id
}

resource "azurerm_key_vault_secret" "jwt_secret" {
  name         = "jwt-secret"
  value        = var.jwt_secret
  key_vault_id = azurerm_key_vault.main.id
}

# ─── Container App: Backend ──────────────────────────────────────────────────
resource "azurerm_container_app" "backend" {
  name                         = "ca-${local.name_prefix}-backend"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"
  tags                         = local.common_tags

  template {
    container {
      name   = "sifap-backend"
      image  = var.backend_image
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "azure"
      }
      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${azurerm_postgresql_flexible_server.main.fqdn}:5432/sifap?sslmode=require"
      }
      env {
        name  = "SPRING_DATASOURCE_USERNAME"
        value = "sifapadmin"
      }
      env {
        name        = "SPRING_DATASOURCE_PASSWORD"
        secret_name = "pg-password"
      }
      env {
        name        = "SIFAP_JWT_SECRET"
        secret_name = "jwt-secret"
      }
    }
    min_replicas = 1
    max_replicas = 3
  }

  secret {
    name  = "pg-password"
    value = random_password.pg.result
  }
  secret {
    name  = "jwt-secret"
    value = var.jwt_secret
  }

  ingress {
    external_enabled = true
    target_port      = 8080
    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
}

# ─── Container App: Frontend ─────────────────────────────────────────────────
resource "azurerm_container_app" "frontend" {
  name                         = "ca-${local.name_prefix}-frontend"
  container_app_environment_id = azurerm_container_app_environment.main.id
  resource_group_name          = azurerm_resource_group.main.name
  revision_mode                = "Single"
  tags                         = local.common_tags

  template {
    container {
      name   = "sifap-frontend"
      image  = var.frontend_image
      cpu    = 0.25
      memory = "0.5Gi"

      env {
        name  = "NEXT_PUBLIC_API_BASE_URL"
        value = "https://${azurerm_container_app.backend.ingress[0].fqdn}"
      }
    }
    min_replicas = 1
    max_replicas = 2
  }

  ingress {
    external_enabled = true
    target_port      = 3000
    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
}
