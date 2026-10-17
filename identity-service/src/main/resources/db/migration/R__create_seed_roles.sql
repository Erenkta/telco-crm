-- Temel Rol Yapılandırması (Telco CRM Aktörleri)
INSERT INTO roles (id, name, description) VALUES
                                              (gen_random_uuid(), 'ROLE_CUSTOMER', 'Kendi hesabını, faturalarını ve taleplerini self-servis olarak yönetir'),
                                              (gen_random_uuid(), 'ROLE_CALL_CENTER_AGENT', 'Müşteri taleplerini karşılar, sipariş ve destek talebi oluşturur'),
                                              (gen_random_uuid(), 'ROLE_FIELD_DEALER', 'Sahada yeni müşteri kaydı ve sipariş oluşturma işlemlerini yürütür'),
                                              (gen_random_uuid(), 'ROLE_MARKETING_MANAGER', 'Kampanya ve ürün kataloğu yönetimi yapar'),
                                              (gen_random_uuid(), 'ROLE_SYSTEM_ADMIN', 'Sistem genelinde kullanıcı, rol ve yetki yönetimini yürütür'),
                                              (gen_random_uuid(), 'ROLE_BILLING_OPERATOR', 'Fatura oluşturma, düzeltme ve tahsilat işlemlerini yönetir'),
                                              (gen_random_uuid(), 'ROLE_SYSTEM_SERVICE', 'Servisler arası otomatik (service-to-service) işlemleri gerçekleştirir')
ON CONFLICT (name) DO NOTHING; -- I have added ON CONFLICT Because these roles are being added in V1__init.sql


