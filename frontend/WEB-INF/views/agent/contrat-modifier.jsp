<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Modifier le contrat n° <c:out value="${contrat.id}"/> · Billy Assurance</title>
</head>
<body>
  <%@ include file="/WEB-INF/views/common/header.jspf" %>

  <main id="contenu" class="page">
    <div class="conteneur apparition">
      <div>
        <a class="retour" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${contrat.id}">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Contrat n° <c:out value="${contrat.id}"/>
        </a>
        <div class="entete-page">
          <div>
            <h1>Modifier le contrat n° <c:out value="${contrat.id}"/></h1>
            <p class="sous-titre">Le contrat n'est pas encore payé : le montant et l'échéance du paiement suivront ces modifications.</p>
          </div>
        </div>
      </div>

      <c:if test="${not empty erreurs.global}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.global}"/></div>
        </div>
      </c:if>

      <form id="formulaire-contrat" class="disposition-laterale" method="post" data-duree="${dureeEnMois}"
            action="${pageContext.request.contextPath}/agent/contrats/modifier" novalidate>
        <input type="hidden" name="id" value="${contrat.id}">

        <div class="pile">
          <section class="carte">
            <div class="carte-entete">
              <div><h2>Garanties</h2><p>Celles que propose le type <c:out value="${contrat.typeAssurance.nom}"/> : au moins une.</p></div>
              <button type="button" class="bouton bouton-petit avec-js" data-tout-cocher>Tout sélectionner</button>
            </div>
            <fieldset class="carte-corps ${not empty erreurs.garanties ? 'a-erreur' : ''}">
              <legend class="visuellement-cache">Garanties du contrat</legend>
              <div class="choix-cases">
                <c:forEach var="g" items="${garantiesDisponibles}">
                  <label class="case-carte" for="garantie${g.id}">
                    <input type="checkbox" id="garantie${g.id}" name="garanties" value="${g.id}"
                           ${garantiesCochees.contains(g.id) ? 'checked' : ''}>
                    <span>
                      <span class="case-titre"><c:out value="${g.nom}"/></span>
                      <span class="case-detail">Plafond <app:euros valeur="${g.plafond}" vide="aucun"/> · franchise <app:euros valeur="${g.franchise}" vide="aucune"/></span>
                    </span>
                  </label>
                </c:forEach>
              </div>
              <c:if test="${not empty erreurs.garanties}"><p class="message-erreur" style="margin-top:10px"><c:out value="${erreurs.garanties}"/></p></c:if>
            </fieldset>
          </section>

          <section class="carte">
            <div class="carte-entete"><div><h2>Période et prime</h2><p>Le contrat dure ${dureeEnMois} mois : la date de fin est calculée à partir de la date de début.</p></div></div>
            <div class="carte-corps formulaire">
              <div class="champ ${not empty erreurs.dateDebut ? 'a-erreur' : ''}">
                <label for="dateDebut">Date de début</label>
                <input class="saisie" type="date" id="dateDebut" name="dateDebut" value="<c:out value='${dateDebut}'/>" min="${aujourdhui}">
                <span class="aide" data-fin-calculee>Fin du contrat ${dureeEnMois} mois après la date de début.</span>
                <c:if test="${not empty erreurs.dateDebut}"><span class="message-erreur"><c:out value="${erreurs.dateDebut}"/></span></c:if>
              </div>
              <div class="champ ${not empty erreurs.prime ? 'a-erreur' : ''}">
                <label for="prime">Prime annuelle</label>
                <div class="saisie-groupe">
                  <input class="saisie" type="text" inputmode="decimal" id="prime" name="prime"
                         value="<c:out value='${prime}'/>" data-minimum="${contrat.typeAssurance.prixBase}">
                  <span class="saisie-suffixe">€</span>
                </div>
                <span class="aide">Minimum <app:euros valeur="${contrat.typeAssurance.prixBase}"/> (prix de base du type d'assurance).</span>
                <span class="message-avertissement" data-alerte-prime hidden>
                  <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>Prime inférieure au prix de base : la modification sera refusée.
                </span>
                <c:if test="${not empty erreurs.prime}"><span class="message-erreur"><c:out value="${erreurs.prime}"/></span></c:if>
              </div>
            </div>
          </section>
        </div>

        <%-- Client et type affichés sans pouvoir être changés : sinon, c'est un autre contrat --%>
        <aside class="carte recap">
          <div class="carte-entete"><h2>Récapitulatif</h2></div>
          <div class="carte-corps">
            <dl>
              <div><dt>Client</dt><dd><c:out value="${contrat.client.utilisateur.prenom} ${contrat.client.utilisateur.nom}"/></dd></div>
              <div><dt>Type d'assurance</dt><dd><c:out value="${contrat.typeAssurance.nom}"/></dd></div>
              <div class="avec-js"><dt>Garanties</dt><dd data-recap="garanties">Aucune</dd></div>
              <div class="avec-js"><dt>Période</dt><dd data-recap="periode">À définir</dd></div>
              <div class="recap-total avec-js"><span class="texte-doux">Prime annuelle</span><strong class="nombre" data-recap="prime">—</strong></div>
            </dl>
          </div>
          <div class="carte-pied">
            <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/agent/contrats/detail?id=${contrat.id}">Annuler</a>
            <button type="submit" class="bouton bouton-principal">
              <svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Enregistrer
            </button>
          </div>
        </aside>
      </form>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
