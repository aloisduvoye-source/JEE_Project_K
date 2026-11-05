<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Billy Assurance · Votre assurance en ligne</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu">
    <section class="hero">
      <div class="conteneur hero-grille">
        <div class="apparition">
          <span class="sur-titre"><svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg>Assurance 100 % en ligne</span>
          <h1>Votre assurance, <span class="degrade">simple et transparente</span>.</h1>
          <p class="hero-texte">
            Consultez vos contrats et vos garanties, déclarez un sinistre et suivez vos paiements
            depuis un seul espace, à tout moment.
          </p>
          <div class="hero-actions">
            <c:choose>
              <c:when test="${not empty sessionScope.utilisateur}">
                <a class="bouton bouton-principal bouton-grand" href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">
                  Accéder à mon espace <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                </a>
              </c:when>
              <c:otherwise>
                <a class="bouton bouton-principal bouton-grand" href="${pageContext.request.contextPath}/inscription">
                  Créer mon compte <svg class="icone" aria-hidden="true"><use href="#i-fleche-droite"/></svg>
                </a>
                <a class="bouton bouton-grand" href="${pageContext.request.contextPath}/connexion">Se connecter</a>
              </c:otherwise>
            </c:choose>
          </div>
          <ul class="hero-points">
            <li><svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Espace personnel sécurisé</li>
            <li><svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Suivi des sinistres</li>
            <li><svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Assistant disponible à toute heure</li>
          </ul>
        </div>

        <%-- Illustration : aperçu (fictif) de l'espace client --%>
        <div class="hero-visuel" aria-hidden="true">
          <div class="maquette maquette-contrat">
            <div class="maquette-ligne">
              <div class="maquette-titre">
                <span class="pastille ton-primaire"><svg class="icone"><use href="#i-maison"/></svg></span>
                <div><strong>Assurance habitation</strong><small>Contrat n° 1024</small></div>
              </div>
              <span class="badge badge-point ton-succes">Actif</span>
            </div>
            <div>
              <div class="progression-libelle"><span>Période de couverture</span><span>64 %</span></div>
              <div class="progression"><span style="width:64%"></span></div>
            </div>
            <div class="etiquettes">
              <span class="etiquette">Responsabilité civile</span>
              <span class="etiquette">Incendie</span>
              <span class="etiquette">Dégâts des eaux</span>
              <span class="etiquette">Vol</span>
            </div>
            <div class="maquette-prix"><span>Prime annuelle</span><strong>250,00 €</strong></div>
          </div>

          <div class="maquette maquette-sinistre">
            <div class="maquette-ligne">
              <strong>Sinistre n° 87</strong>
              <span class="badge ton-attention">En analyse</span>
            </div>
            <small class="texte-doux">Dégâts des eaux · déclaré le 2 nov.</small>
            <div class="maquette-etapes"><span class="faite"></span><span class="faite"></span><span></span><span></span></div>
          </div>

          <div class="maquette maquette-paiement">
            <span class="pastille pastille-petite ton-succes"><svg class="icone"><use href="#i-coche"/></svg></span>
            <div><strong>Paiement reçu</strong><small>250,00 € · Carte bancaire</small></div>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="conteneur">
        <div class="section-entete">
          <h2>Tout votre contrat, au même endroit</h2>
          <p>Un seul espace pour gérer votre assurance, du premier contrat à l'indemnisation.</p>
        </div>
        <div class="grille grille-3 apparition">
          <div class="tuile">
            <span class="pastille ton-primaire"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <h3>Vos contrats</h3>
            <p>Automobile, habitation, santé, voyage ou professionnelle : retrouvez chaque contrat, sa période et sa prime.</p>
          </div>
          <div class="tuile">
            <span class="pastille ton-succes"><svg class="icone" aria-hidden="true"><use href="#i-bouclier-ok"/></svg></span>
            <h3>Vos garanties</h3>
            <p>Plafonds et franchises de chaque garantie, présentés clairement, sans petites lignes.</p>
          </div>
          <div class="tuile">
            <span class="pastille ton-attention"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg></span>
            <h3>Vos sinistres</h3>
            <p>Déclarez un sinistre en ligne, joignez vos justificatifs et suivez son traitement étape par étape.</p>
          </div>
          <div class="tuile">
            <span class="pastille ton-info"><svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg></span>
            <h3>Vos paiements</h3>
            <p>Échéances à venir, paiements effectués et historique complet.</p>
          </div>
          <div class="tuile">
            <span class="pastille ton-violet"><svg class="icone" aria-hidden="true"><use href="#i-etincelles"/></svg></span>
            <h3>Un assistant intelligent</h3>
            <p>Posez vos questions sur vos contrats et vos garanties, et obtenez une réponse immédiate.</p>
          </div>
          <div class="tuile">
            <span class="pastille ton-neutre"><svg class="icone" aria-hidden="true"><use href="#i-cadenas"/></svg></span>
            <h3>Vos données protégées</h3>
            <p>Mots de passe chiffrés et accès réservé : chacun ne voit que ce qui le concerne.</p>
          </div>
        </div>
      </div>
    </section>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
