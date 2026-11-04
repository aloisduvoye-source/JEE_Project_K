## Ce que fait cette Pull Request
<!-- 2-3 phrases sur la fonctionnalité ou le correctif. Une seule mission par branche. -->

## Comment la tester
<!-- Les étapes précises : commandes, pages à ouvrir, comptes de test utilisés. -->

## Checklist avant de demander une relecture
- [ ] `mvn -f jee-app/pom.xml clean package` affiche BUILD SUCCESS
- [ ] Le code est commenté en français (commentaires + Javadoc)
- [ ] La logique métier est dans des classes (services) ; les servlets restent minces
- [ ] Une seule fonctionnalité par branche
- [ ] Si la base a changé : schema.sql ET dump.sql mis à jour ensemble
- [ ] Aucun mot de passe ni clé d'API dans le dépôt (.env non commité)
