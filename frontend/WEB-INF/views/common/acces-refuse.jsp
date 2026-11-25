<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Accès refusé · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="auth">
    <div class="vide apparition">
      <span class="erreur-code">403</span>
      <h1>Accès refusé</h1>
      <p>Cette page est réservée à un autre profil d'utilisateur.</p>
      <c:choose>
        <c:when test="${not empty sessionScope.utilisateur}">
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}${sessionScope.utilisateur.role.accueil}">
            <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Retour à mon espace
          </a>
        </c:when>
        <c:otherwise>
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/">
            <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Retour à l'accueil
          </a>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
