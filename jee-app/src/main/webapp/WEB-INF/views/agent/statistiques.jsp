<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Statistiques · Billy Assurance</title>
  <%-- Chargement de Chart.js pour les graphiques (ajouté avec l'aide de l'IA).
       Version fixée : sans elle, le CDN sert toujours la dernière, qui peut changer et casser la page --%>
  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <%-- Page partagée agent / admin : retour vers l'espace du rôle connecté --%>
        <a class="retour" href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Tableau de bord
        </a>
        <div class="entete-page">
          <div>
            <h1>Statistiques</h1>
            <p class="sous-titre">Clients, contrats et finances de l'assurance, avec une synthèse par l'assistant IA.</p>
          </div>
        </div>
      </div>

      <section class="carte" id="analyse">
        <div class="carte-entete">
          <div class="titre-icone">
            <span class="pastille pastille-petite ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg></span>
            <div>
              <h2>Analyse intelligente</h2>
              <p>Synthèse des statistiques par l'assistant IA Gemini. Une même synthèse est réutilisée pendant 10 minutes.</p>
            </div>
          </div>
          <%-- POST : recharger la page ne relance pas l'appel à l'API --%>
          <form method="post" action="${pageContext.request.contextPath}/agent/statistiques" id="formAnalyse">
            <button type="submit" class="bouton bouton-principal">
              <svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg>Générer une synthèse
            </button>
          </form>
        </div>
        <div class="carte-corps" id="analyseEnCours" hidden>
          <p class="texte-doux" role="status">Analyse en cours : cela peut prendre jusqu'à une minute…</p>
        </div>
        <%-- Synthèse gardée en session par StatistiquesServlet.doPost : affichée une fois puis effacée --%>
        <c:if test="${not empty sessionScope.analyseIa}">
          <div class="carte-corps analyse-ia"><c:out value="${sessionScope.analyseIa}"/></div>
          <c:remove var="analyseIa" scope="session"/>
        </c:if>
      </section>

      <%-- Clients --%>
      <div class="titre-section"><h2>Clients</h2></div>
      <div class="grille grille-stats">
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Clients</span><span class="pastille pastille-petite ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-utilisateurs"/></svg></span></div>
          <div class="stat-valeur nombre"><c:out value="${totalClients}"/></div>
          <div class="stat-detail">Au total</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Nouveaux clients</span><span class="pastille pastille-petite ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-plus"/></svg></span></div>
          <div class="stat-valeur nombre"><c:out value="${nouveauxClients}"/></div>
          <div class="stat-detail">Ce mois-ci</div>
        </div>
      </div>
      <section class="carte espace-haut-petit">
        <div class="carte-entete">
          <div><h3>Répartition par type d'assurance</h3><p>Clients ayant un contrat actif de ce type.</p></div>
        </div>
        <div class="carte-corps grille grille-2">
          <%-- Zone d'affichage du camembert Chart.js (généré avec l'aide de l'IA) --%>
          <div class="graphique"><canvas id="chartClients" aria-label="Camembert de la répartition des clients" role="img"></canvas></div>
          <div class="tableau-conteneur">
            <table class="tableau" id="tableClients">
              <thead><tr><th>Type d'assurance</th><th class="a-droite">Clients</th></tr></thead>
              <tbody>
                <c:forEach var="ligne" items="${repartitionClients}">
                  <tr data-libelle="<c:out value='${ligne[0]}'/>" data-valeur="<c:out value='${ligne[1]}'/>">
                    <td><c:out value="${ligne[0]}"/></td>
                    <td class="a-droite nombre"><c:out value="${ligne[1]}"/></td>
                  </tr>
                </c:forEach>
                <c:if test="${empty repartitionClients}">
                  <tr class="ligne-message"><td colspan="2">Aucune donnée disponible.</td></tr>
                </c:if>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <%-- Contrats --%>
      <div class="titre-section"><h2>Contrats</h2></div>
      <div class="grille grille-stats">
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Contrats actifs</span><span class="pastille pastille-petite ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span></div>
          <div class="stat-valeur nombre"><c:out value="${contratsActifs}"/></div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Contrats expirés</span><span class="pastille pastille-petite ton-neutre"><svg class="icone" aria-hidden="true"><use href="#i-horloge"/></svg></span></div>
          <div class="stat-valeur nombre"><c:out value="${contratsExpires}"/></div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Contrats résiliés</span><span class="pastille pastille-petite ton-danger"><svg class="icone" aria-hidden="true"><use href="#i-archive"/></svg></span></div>
          <div class="stat-valeur nombre"><c:out value="${contratsResilies}"/></div>
        </div>
      </div>
      <section class="carte espace-haut-petit">
        <div class="carte-entete">
          <div><h3>Évolution du nombre de contrats actifs</h3><p>Contrats actifs en même temps, en fin de mois (aujourd'hui pour le mois en cours).</p></div>
        </div>
        <div class="carte-corps pile">
          <%-- Zone d'affichage de la courbe Chart.js (généré avec l'aide de l'IA) --%>
          <div class="graphique"><canvas id="chartContrats" aria-label="Courbe des contrats actifs par mois" role="img"></canvas></div>
          <details>
            <summary>Voir les données</summary>
            <div class="tableau-conteneur">
              <table class="tableau" id="tableContrats">
                <thead><tr><th>Mois</th><th class="a-droite">Contrats actifs</th></tr></thead>
                <tbody>
                  <c:forEach var="ligne" items="${evolutionContrats}">
                    <tr data-libelle="<c:out value='${ligne[0]}'/>" data-valeur="<c:out value='${ligne[1]}'/>">
                      <td class="nombre"><c:out value="${ligne[0]}"/></td>
                      <td class="a-droite nombre"><c:out value="${ligne[1]}"/></td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty evolutionContrats}">
                    <tr class="ligne-message"><td colspan="2">Aucun contrat enregistré.</td></tr>
                  </c:if>
                </tbody>
              </table>
            </div>
          </details>
        </div>
      </section>

      <%-- Finances --%>
      <div class="titre-section"><h2>Finances</h2></div>
      <div class="grille grille-stats">
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Chiffre d'affaires net</span><span class="pastille pastille-petite ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-euro"/></svg></span></div>
          <div class="stat-valeur nombre"><app:euros valeur="${chiffreAffairesNet}"/></div>
          <div class="stat-detail">Encaissé moins les remboursements</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Encaissé</span><span class="pastille pastille-petite ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span></div>
          <div class="stat-valeur nombre"><app:euros valeur="${chiffreAffaires}"/></div>
          <div class="stat-detail">Paiements effectués, avant remboursements</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Remboursements</span><span class="pastille pastille-petite ton-orange"><svg class="icone" aria-hidden="true"><use href="#i-restaurer"/></svg></span></div>
          <div class="stat-valeur nombre"><app:euros valeur="${totalRemboursements}"/></div>
          <div class="stat-detail">Versés aux résiliations</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Primes actives</span><span class="pastille pastille-petite ton-info"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span></div>
          <div class="stat-valeur nombre"><app:euros valeur="${totalPrimes}"/></div>
          <div class="stat-detail">Total des contrats actifs</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Indemnisations</span><span class="pastille pastille-petite ton-attention"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span></div>
          <div class="stat-valeur nombre"><app:euros valeur="${totalIndemnisations}"/></div>
          <div class="stat-detail">Versées aux clients</div>
        </div>
        <div class="carte stat">
          <div class="stat-entete"><span class="stat-libelle">Indemnisations / primes</span><span class="pastille pastille-petite ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-indicateurs"/></svg></span></div>
          <c:choose>
            <c:when test="${empty ratioIndemnisations}"><div class="stat-valeur en-attente">—</div></c:when>
            <c:otherwise><div class="stat-valeur nombre"><c:out value="${ratioIndemnisations}"/> %</div></c:otherwise>
          </c:choose>
          <div class="stat-detail">Sur le chiffre d'affaires net</div>
        </div>
      </div>
      <section class="carte espace-haut-petit">
        <div class="carte-entete">
          <div><h3>Évolution mensuelle des flux financiers</h3><p>Argent encaissé (paiements effectués) et versé aux clients (indemnisations, remboursements), mois par mois.</p></div>
        </div>
        <div class="carte-corps pile">
          <%-- Zone d'affichage de l'histogramme Chart.js (généré avec l'aide de l'IA) --%>
          <div class="graphique"><canvas id="chartRevenus" aria-label="Histogramme des flux financiers par mois" role="img"></canvas></div>
          <details>
            <summary>Voir les données</summary>
            <div class="tableau-conteneur">
              <table class="tableau" id="tableRevenus">
                <thead>
                  <tr><th>Mois</th><th class="a-droite">Encaissé</th><th class="a-droite">Indemnisations</th><th class="a-droite">Remboursements</th></tr>
                </thead>
                <tbody>
                  <c:forEach var="ligne" items="${evolutionFinances}">
                    <tr data-libelle="<c:out value='${ligne[0]}'/>" data-encaisse="<c:out value='${ligne[1]}'/>"
                        data-indemnise="<c:out value='${ligne[2]}'/>" data-rembourse="<c:out value='${ligne[3]}'/>">
                      <td class="nombre"><c:out value="${ligne[0]}"/></td>
                      <td class="a-droite nombre"><app:euros valeur="${ligne[1]}"/></td>
                      <td class="a-droite nombre"><app:euros valeur="${ligne[2]}"/></td>
                      <td class="a-droite nombre"><app:euros valeur="${ligne[3]}"/></td>
                    </tr>
                  </c:forEach>
                  <c:if test="${empty evolutionFinances}">
                    <tr class="ligne-message"><td colspan="4">Aucun paiement ni versement.</td></tr>
                  </c:if>
                </tbody>
              </table>
            </div>
          </details>
        </div>
      </section>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>

  <%-- Script Chart.js pour dessiner les 3 graphiques (généré avec l'aide de l'IA) --%>
  <script>
    // Pendant l'analyse IA (jusqu'à une minute) : bouton désactivé, pour ne pas relancer la demande
    document.getElementById('formAnalyse').addEventListener('submit', function () {
      this.querySelector('button').disabled = true;
      document.getElementById('analyseEnCours').hidden = false;
    });

    // Libellés et valeurs lus dans les lignes du tableau (attributs data-) : c:out dans un attribut HTML
    // est décodé par le navigateur, alors que dans le script une apostrophe resterait affichée "&#039;"
    // attributs : noms des valeurs à lire (data-valeur par défaut), une liste de nombres par nom
    function donnees(idTableau, attributs = ['valeur']) {
      const lignes = document.querySelectorAll('#' + idTableau + ' tbody tr[data-libelle]');
      const resultat = {libelles: Array.from(lignes, ligne => ligne.dataset.libelle)};
      for (const attribut of attributs) {
        resultat[attribut] = Array.from(lignes, ligne => Number(ligne.dataset[attribut]));
      }
      return resultat;
    }

    // Couleurs prises dans les variables de app.css : les graphiques suivent le thème clair ou sombre
    function couleur(nom) {
      return getComputedStyle(document.documentElement).getPropertyValue('--' + nom).trim();
    }

    function dessiner() {
      const palette = ['primaire', 'succes', 'attention', 'violet', 'info', 'orange', 'danger'].map(couleur);
      Chart.defaults.color = couleur('texte-doux');
      Chart.defaults.borderColor = couleur('bordure');
      Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
      Chart.defaults.maintainAspectRatio = false;
      const graphiques = [];

      // 1. Camembert : répartition des clients par type d'assurance (aide de l'ia)
      const clients = donnees('tableClients');
      graphiques.push(new Chart(document.getElementById('chartClients'), {
        type: 'pie',
        data: {
          labels: clients.libelles,
          datasets: [{data: clients.valeur, backgroundColor: palette, borderColor: couleur('surface')}]
        }
      }));

      // 2. Courbe : nombre de contrats actifs en même temps, mois par mois (aide de l'ia)
      const contrats = donnees('tableContrats');
      graphiques.push(new Chart(document.getElementById('chartContrats'), {
        type: 'line',
        data: {
          labels: contrats.libelles,
          datasets: [{label: 'Contrats actifs', data: contrats.valeur, borderColor: palette[0], backgroundColor: palette[0], tension: .3}]
        },
        options: {scales: {y: {beginAtZero: true, ticks: {precision: 0}}}}
      }));

      // 3. Barres : argent encaissé et versé par mois (aide de l'ia)
      const finances = donnees('tableRevenus', ['encaisse', 'indemnise', 'rembourse']);
      graphiques.push(new Chart(document.getElementById('chartRevenus'), {
        type: 'bar',
        data: {
          labels: finances.libelles,
          datasets: [
            {label: 'Encaissé (€)', data: finances.encaisse, backgroundColor: palette[1]},
            {label: 'Indemnisations (€)', data: finances.indemnise, backgroundColor: palette[2]},
            {label: 'Remboursements (€)', data: finances.rembourse, backgroundColor: palette[5]}
          ]
        },
        options: {scales: {y: {beginAtZero: true}}}
      }));
      return graphiques;
    }

    // Chart.js absent (CDN injoignable) : les tableaux restent lisibles
    if (window.Chart) {
      let graphiques = dessiner();
      // Changement de thème avec le bouton de la barre : graphiques redessinés avec les nouvelles couleurs
      new MutationObserver(function () {
        graphiques.forEach(g => g.destroy());
        graphiques = dessiner();
      }).observe(document.documentElement, {attributes: true, attributeFilter: ['data-theme']});
    }
  </script>
</body>
</html>
