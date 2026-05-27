output "resource_group_name" {
  description = "Nome do Resource Group criado"
  value       = azurerm_resource_group.main.name
}

output "backend_url" {
  description = "URL pública do backend"
  value       = "https://${azurerm_container_app.backend.ingress[0].fqdn}"
}

output "frontend_url" {
  description = "URL pública do frontend"
  value       = "https://${azurerm_container_app.frontend.ingress[0].fqdn}"
}

output "postgres_fqdn" {
  description = "FQDN do PostgreSQL Flexible Server"
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "key_vault_uri" {
  description = "URI do Key Vault"
  value       = azurerm_key_vault.main.vault_uri
}
