<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Contrat n° <c:out value="${contrat.id}"/> · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/client/contrats">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Mes contrats
        </a>
        <div class="entete-page">
          <div>
            <h1>Contrat n° <c:out value="${contrat.id}"/></h1>
            <p class="sous-titre"><c:out value="${contrat.typeAssurance.nom}"/></p>
          </div>
        </div>
      </div>

      <%@ include file="/WEB-INF/views/common/messages-contrat.jspf" %>

      <div class="disposition-laterale">
        <div class="pile">
          <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>
        </div>
        <aside class="pile">
          <%@ include file="/WEB-INF/views/common/contrat-paiement.jspf" %>

          <%-- Formulaire tant que le contrat peut encore être payé : PaiementService refuse de toute façon
               un contrat déjà payé, résilié ou expiré --%>
          <c:if test="${not empty paiement
                        and (paiement.statut == 'EN_ATTENTE' or paiement.statut == 'EN_RETARD')
                        and contrat.statut != 'RESILIE' and contrat.statut != 'EXPIRE'}">
            <section class="carte">
              <div class="carte-entete">
                <div><h2>Payer mon contrat</h2><p>Votre contrat sera actif à sa date de début, ou dès maintenant si elle est passée.</p></div>
              </div>
              <form class="carte-corps formulaire" method="post" action="${pageContext.request.contextPath}/client/contrats/paiement">
                <input type="hidden" name="id" value="${contrat.id}">
                <fieldset class="champ">
                  <legend class="libelle">Mode de paiement</legend>
                  <div class="choix-cases" style="grid-template-columns:repeat(2, minmax(0, 1fr))">
                    <c:forEach var="m" items="${modes}">
                      <label class="case-carte" for="mode-${m}">
                        <input type="radio" id="mode-${m}" name="mode" value="${m}" required>
                        <span class="case-titre"><app:modePaiement valeur="${m}"/></span>
                      </label>
                    </c:forEach>
                  </div>
                </fieldset>
                <div class="actions-formulaire">
                  <button type="submit" class="bouton bouton-principal bouton-bloc">
                    <svg class="icone" aria-hidden="true"><use href="#i-carte"/></svg>Payer <app:euros valeur="${paiement.montant}"/>
                  </button>
                </div>
              </form>
            </section>
          </c:if>

          <%-- Un contrat payé et commencé peut couvrir un sinistre (SinistreService vérifie la date du sinistre) --%>
          <c:if test="${paiement.statut == 'EFFECTUE' and contrat.statut != 'EN_ATTENTE'}">
            <section class="carte">
              <div class="carte-entete">
                <div><h2>Un sinistre ?</h2><p>Déclarez-le sur ce contrat et suivez son traitement en ligne.</p></div>
              </div>
              <div class="carte-corps">
                <a class="bouton bouton-principal bouton-bloc" href="${pageContext.request.contextPath}/client/sinistres/nouveau?contrat=${contrat.id}">
                  <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>Déclarer un sinistre
                </a>
              </div>
            </section>
          </c:if>

          <%@ include file="/WEB-INF/views/common/contrat-renouvellement.jspf" %>
          <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>
        </aside>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
