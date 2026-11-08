<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
        <a class="retour" href="${pageContext.request.contextPath}/agent/contrats">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Contrats
        </a>
        <div class="entete-page">
          <div>
            <h1>Contrat n° <c:out value="${contrat.id}"/></h1>
            <p class="sous-titre"><c:out value="${contrat.typeAssurance.nom}"/> · <c:out value="${contrat.client.utilisateur.prenom} ${contrat.client.utilisateur.nom}"/></p>
          </div>
          <%-- Modification par l'agent seulement, tant que le contrat n'est pas payé (ModifierContratServlet le vérifie aussi) --%>
          <c:if test="${sessionScope.utilisateur.role == 'AGENT'
                        and (contrat.statut == 'EN_ATTENTE' or contrat.statut == 'SUSPENDU')
                        and (empty paiement or paiement.statut != 'EFFECTUE')}">
            <div class="actions">
              <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/agent/contrats/modifier?id=${contrat.id}">
                <svg class="icone" aria-hidden="true"><use href="#i-crayon"/></svg>Modifier le contrat
              </a>
            </div>
          </c:if>
        </div>
      </div>

      <%@ include file="/WEB-INF/views/common/messages-contrat.jspf" %>

      <div class="disposition-laterale">
        <div class="pile">
          <%@ include file="/WEB-INF/views/common/contrat-infos.jspf" %>
        </div>
        <aside class="pile">
          <%-- Le paiement est affiché sans formulaire : c'est le client qui paie, depuis sa propre fiche --%>
          <%@ include file="/WEB-INF/views/common/contrat-paiement.jspf" %>
          <%@ include file="/WEB-INF/views/common/contrat-resiliation.jspf" %>
        </aside>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
