<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Sinistre n° <c:out value="${sinistre.id}"/> · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/client/sinistres">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Mes sinistres
        </a>
        <div class="entete-page">
          <div>
            <h1>Sinistre n° <c:out value="${sinistre.id}"/></h1>
            <p class="sous-titre"><app:typeSinistre valeur="${sinistre.typeSinistre}"/> · <c:out value="${sinistre.contrat.typeAssurance.nom}"/></p>
          </div>
        </div>
      </div>

      <%@ include file="/WEB-INF/views/common/messages-sinistre.jspf" %>

      <div class="disposition-laterale">
        <div class="pile">
          <%@ include file="/WEB-INF/views/common/sinistre-infos.jspf" %>
          <%@ include file="/WEB-INF/views/common/sinistre-pieces.jspf" %>
          <%@ include file="/WEB-INF/views/common/sinistre-discussion.jspf" %>
        </div>
        <aside class="pile">
          <%@ include file="/WEB-INF/views/common/sinistre-suivi.jspf" %>
        </aside>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
