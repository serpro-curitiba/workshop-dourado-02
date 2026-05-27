variable "environment" {
  description = "Ambiente de deploy (dev, stage, prod)"
  type        = string
  default     = "dev"
  validation {
    condition     = contains(["dev", "stage", "prod"], var.environment)
    error_message = "environment deve ser dev, stage ou prod."
  }
}

variable "location" {
  description = "Região Azure"
  type        = string
  default     = "brazilsouth"
}

variable "owner" {
  description = "Responsável pelo ambiente (email ou squad)"
  type        = string
  default     = "workshop-dourado-02"
}

variable "backend_image" {
  description = "Imagem Docker do backend (registry/repo:tag)"
  type        = string
  default     = "ghcr.io/workshop-dourado-02/sifap-backend:latest"
}

variable "frontend_image" {
  description = "Imagem Docker do frontend (registry/repo:tag)"
  type        = string
  default     = "ghcr.io/workshop-dourado-02/sifap-frontend:latest"
}

variable "jwt_secret" {
  description = "Segredo JWT (mínimo 32 chars) — fornecido via TF_VAR_jwt_secret ou -var"
  type        = string
  sensitive   = true
}
