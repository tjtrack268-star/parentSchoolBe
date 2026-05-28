# Diagramme de Classes - Parents School

## Entités Principales

### User (Membre)
- id, email, firstName, lastName, phone
- type (ORDINARY, HONOR, BENEFACTOR)
- status (ACTIVE, SUSPENDED, FOCAL_POINT)
- country, city, createdAt
- totalPoints, currentGrade, sponsor

### Sponsorship (Parrainage)
- id, sponsor, sponsored
- sponsorshipCode (unique)
- createdAt, status, pointsEarned

### Grade
- id, name, requiredSponsorships, requiredPoints
- benefitAmount, commissionRate, level

### Commission
- id, beneficiary, source, amount
- type, generationLevel, createdAt

### Payment
- id, payer, amount, method
- status, type, createdAt

### SessionVoucher (Bon Formation)
- id, owner, voucherCode, value
- status, transferredTo, createdAt

### Country
- id, name, code, currency, focalPoint

## Relations Critiques

### Parrainage Multi-Niveaux
```
User 1--* Sponsorship (as sponsor)
User 1--* Sponsorship (as sponsored)
User *--1 User (sponsor relationship)
```

### Grades et Commissions
```
User *--1 Grade (current grade)
Payment 1--* Commission (generates)
Sponsorship 1--1 Payment (triggers)
```

### Règles Métier Intégrées

**Calcul Automatique Points:**
- 60 points par parrainage direct
- Cumul pour détermination grade

**Grades (parrainages requis):**
- Leader: 4 parrainages, 240 points
- Leader Senior: 8 parrainages, 1200 points  
- Coordinateur: 18 parrainages, 3000 points
- Mentor: 30 parrainages, 10000 points
- Directeur: 40 parrainages, 30000 points

**Commissions Multi-Niveaux:**
- Coordinateur: 5% équipe jusqu'à 5ème génération
- Mentor: 10% direct + 5% équipe jusqu'à 5ème génération
- Directeur: 15% direct + 5% équipe jusqu'à 5ème génération

**Contraintes:**
- Code parrainage obligatoire inscription
- 1 bon max transférable non-membres
- Validation adhésions (auto/manuelle)