-- Create Core Tenants Table (For Data Isolation)
CREATE TABLE tenants (
                         id VARCHAR(50) PRIMARY KEY,
                         company_name VARCHAR(100) NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Shipments Table
CREATE TABLE shipments (
                           id BIGSERIAL PRIMARY KEY,
                           tenant_id VARCHAR(50) NOT NULL,
                           tracking_number VARCHAR(100) UNIQUE NOT NULL,
                           status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                           weight_lbs NUMERIC(10, 2),
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_shipment_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- Add Index for High-Concurrency Tenant Lookups
CREATE INDEX idx_shipments_tenant ON shipments(tenant_id);