# Analyse Détaillée des Entités - Parents School

## 1. USER (Entité Centrale)

### Attributs Core
- `id`: Long (PK)
- `email`: String (unique, required)
- `password`: String (encoded)
- `firstName`: String (required)
- `lastName`: String (required)
- `phone`: String (unique)

### Attributs Métier
- `userType`: Enum (ORDINARY, HONOR, BENEFACTOR)
- `userStatus`: Enum (ACTIVE, SUSPENDED, FOCAL_POINT)
- `totalPoints`: Integer (calculé automatiquement)
- `sponsorshipCode`: String (unique, généré auto)

### Relations
- `sponsor`: User (self-reference, nullable)
- `currentGrade`: Grade (ManyToOne)
- `country`: Country (ManyToOne)
- `sponsoredUsers`: List<User> (OneToMany)

### Attributs Audit
- `createdAt`: LocalDateTime
- `updatedAt`: LocalDateTime
- `lastLoginAt`: LocalDateTime

## 2. SPONSORSHIP (Relation Critique)

### Attributs
- `id`: Long (PK)
- `sponsorshipCode`: String (unique, index)
- `pointsEarned`: Integer (default 60)
- `status`: Enum (PENDING, VALIDATED, REJECTED)
- `validatedAt`: LocalDateTime
- `validatedBy`: User

### Relations
- `sponsor`: User (ManyToOne, required)
- `sponsored`: User (ManyToOne, required)
- `triggeringPayment`: Payment (OneToOne)

## 3. GRADE (Référentiel)

### Attributs
- `id`: Long (PK)
- `name`: String (LEADER, LEADER_SENIOR, etc.)
- `level`: Integer (1-5)
- `requiredSponsorships`: Integer
- `requiredPoints`: Integer
- `benefitAmount`: BigDecimal (en FCFA)
- `directCommissionRate`: BigDecimal (%)
- `teamCommissionRate`: BigDecimal (%)

### Données Pré-remplies
```
LEADER: 4 parrainages, 240 points, 5000 FCFA
LEADER_SENIOR: 8 parrainages, 1200 points, 10000 FCFA
COORDINATEUR: 18 parrainages, 3000 points, 15000 FCFA, 5% équipe
MENTOR: 30 parrainages, 10000 points, 25000 FCFA, 10% direct + 5% équipe
DIRECTEUR: 40 parrainages, 30000 points, 50000 FCFA, 15% direct + 5% équipe
```

## 4. COMMISSION (Calculs Financiers)

### Attributs
- `id`: Long (PK)
- `amount`: BigDecimal
- `commissionRate`: BigDecimal
- `generationLevel`: Integer (1-5)
- `commissionType`: Enum (DIRECT, TEAM)
- `calculatedAt`: LocalDateTime
- `paidAt`: LocalDateTime (nullable)

### Relations
- `beneficiary`: User (ManyToOne)
- `sourceUser`: User (ManyToOne) // celui qui a généré la commission
- `sourcePayment`: Payment (ManyToOne)

## 5. PAYMENT (Transactions)

### Attributs
- `id`: Long (PK)
- `amount`: BigDecimal (required)
- `currency`: String (default "FCFA")
- `paymentMethod`: Enum (MOBILE_MONEY, BANK_CARD, BANK_TRANSFER)
- `paymentType`: Enum (MEMBERSHIP, DONATION, VOUCHER)
- `status`: Enum (PENDING, COMPLETED, FAILED, REFUNDED)
- `transactionId`: String (external reference)

### Relations
- `payer`: User (ManyToOne)
- `generatedCommissions`: List<Commission> (OneToMany)

## 6. SESSION_VOUCHER (Bons Formation)

### Attributs
- `id`: Long (PK)
- `voucherCode`: String (unique)
- `value`: BigDecimal (default 3000 FCFA)
- `status`: Enum (ACTIVE, USED, TRANSFERRED, EXPIRED)
- `generatedAt`: LocalDateTime
- `usedAt`: LocalDateTime (nullable)

### Relations
- `owner`: User (ManyToOne)
- `transferredTo`: User (ManyToOne, nullable)
- `sourcePayment`: Payment (ManyToOne)

## 7. COUNTRY (Gestion Géographique)

### Attributs
- `id`: Long (PK)
- `name`: String (required)
- `code`: String (ISO, unique)
- `currency`: String (default "FCFA")
- `isActive`: Boolean (default true)

### Relations
- `focalPoint`: User (OneToOne, nullable)
- `members`: List<User> (OneToMany)

## Contraintes et Index Critiques

### Index Obligatoires
- `user.email` (unique)
- `user.sponsorship_code` (unique)
- `sponsorship.sponsorship_code` (unique)
- `user.sponsor_id` (pour requêtes parrainage)

### Contraintes Métier
- Un user ne peut pas se parrainer lui-même
- Code parrainage obligatoire sauf pour admin
- Maximum 1 voucher transférable pour non-membres
- Calcul commission limité à 5 générations