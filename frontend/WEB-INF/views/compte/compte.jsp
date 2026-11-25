<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Mon compte · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur conteneur-etroit apparition">
      <div class="entete-page">
        <div>
          <h1>Mon compte</h1>
          <p class="sous-titre">Vos informations personnelles et votre sécurité.</p>
        </div>
      </div>

      <c:if test="${param.succes == 'profil'}">
        <div class="alerte ton-succes" role="status" data-nettoyer-url="succes">
          <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>
          <div class="alerte-texte">Vos informations ont été mises à jour.</div>
        </div>
      </c:if>
      <c:if test="${param.succes == 'motdepasse'}">
        <div class="alerte ton-succes" role="status" data-nettoyer-url="succes">
          <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>
          <div class="alerte-texte">Votre mot de passe a été modifié.</div>
        </div>
      </c:if>

      <section class="carte">
        <div class="profil-banniere">
          <span class="avatar avatar-grand"><c:out value="${fn:toUpperCase(fn:substring(compte.prenom, 0, 1))}${fn:toUpperCase(fn:substring(compte.nom, 0, 1))}"/></span>
          <div>
            <h2><c:out value="${compte.prenom} ${compte.nom}"/></h2>
            <p class="texte-doux"><c:out value="${compte.email}"/></p>
          </div>
          <span class="badge ton-primaire" style="margin-left:auto">
            ${compte.role == 'ADMIN' ? 'Administrateur' : (compte.role == 'AGENT' ? 'Agent' : 'Client')}
          </span>
        </div>
        <dl class="liste-infos">
          <div><dt>Prénom</dt><dd><c:out value="${compte.prenom}"/></dd></div>
          <div><dt>Nom</dt><dd><c:out value="${compte.nom}"/></dd></div>
          <div><dt>Email</dt><dd><c:out value="${compte.email}"/></dd></div>
          <div><dt>Identifiant</dt><dd><c:out value="${compte.identifiant}"/></dd></div>
        </dl>
        <div class="carte-pied">
          <a class="bouton" href="${pageContext.request.contextPath}/compte/mot-de-passe">
            <svg class="icone" aria-hidden="true"><use href="#i-cle"/></svg>Changer mon mot de passe
          </a>
          <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/compte/profil">
            <svg class="icone" aria-hidden="true"><use href="#i-crayon"/></svg>Modifier mes informations
          </a>
        </div>
      </section>

      <%-- L'administrateur ne peut pas supprimer son compte : il est le seul à gérer l'application --%>
      <c:if test="${compte.role != 'ADMIN'}">
        <section class="carte espace-haut">
          <div class="carte-entete">
            <div>
              <h2>Supprimer mon compte</h2>
              <p>Vous ne pourrez plus vous connecter. Les comptes liés à des contrats sont désactivés plutôt qu'effacés.</p>
            </div>
          </div>
          <div class="carte-pied">
            <a class="bouton bouton-danger-doux" href="${pageContext.request.contextPath}/compte/supprimer">
              <svg class="icone" aria-hidden="true"><use href="#i-poubelle"/></svg>Supprimer mon compte
            </a>
          </div>
        </section>
      </c:if>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
