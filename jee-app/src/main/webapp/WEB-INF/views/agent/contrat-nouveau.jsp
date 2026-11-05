<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Nouveau contrat · Billy Assurance</title>
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
            <h1>Nouveau contrat</h1>
            <p class="sous-titre">Le contrat est créé en attente d'activation.</p>
          </div>
        </div>
        <ol class="etapes">
          <li class="etape ${empty typeChoisi ? 'active' : 'faite'}">
            <span class="etape-numero">
              <c:choose>
                <c:when test="${empty typeChoisi}">1</c:when>
                <c:otherwise><svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg></c:otherwise>
              </c:choose>
            </span>
            Type d'assurance
          </li>
          <li class="etape ${empty typeChoisi ? '' : 'active'}"><span class="etape-numero">2</span>Détails du contrat</li>
        </ol>
      </div>

      <c:if test="${not empty erreurs.global or not empty erreurs.type}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${not empty erreurs.global ? erreurs.global : erreurs.type}"/></div>
        </div>
      </c:if>

      <%-- Étape 1 : le type d'assurance détermine les garanties proposées (lien ?type=..., sans JavaScript) --%>
      <section class="carte">
        <div class="carte-entete">
          <div>
            <h2>Type d'assurance</h2>
            <p>${empty typeChoisi ? "Choisissez le type d'assurance du contrat." : "Vous pouvez encore changer de type : les garanties proposées changeront."}</p>
          </div>
        </div>
        <div class="carte-corps">
          <c:choose>
            <c:when test="${empty types}">
              <p class="texte-doux">Aucun type d'assurance n'est proposé pour l'instant.</p>
            </c:when>
            <c:otherwise>
              <div class="choix-types">
                <c:forEach var="t" items="${types}">
                  <a class="choix-type ${t.id == typeChoisi.id ? 'selectionne' : ''}"
                     href="${pageContext.request.contextPath}/agent/contrats/nouveau?type=${t.id}"
                     ${t.id == typeChoisi.id ? 'aria-current="true"' : ''}>
                    <span class="choix-type-nom">
                      <c:out value="${t.nom}"/>
                      <c:if test="${t.id == typeChoisi.id}"><svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg></c:if>
                    </span>
                    <span class="choix-type-prix">À partir de <b class="nombre"><app:euros valeur="${t.prixBase}"/></b> / an</span>
                    <c:if test="${not empty t.description}">
                      <span class="choix-type-description"><c:out value="${t.description}"/></span>
                    </c:if>
                  </a>
                </c:forEach>
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </section>

      <%-- Étape 2 : affichée une fois le type choisi --%>
      <c:if test="${not empty typeChoisi}">
        <form id="formulaire-contrat" class="disposition-laterale espace-haut" method="post"
              action="${pageContext.request.contextPath}/agent/contrats/nouveau" novalidate>
          <input type="hidden" name="type" value="${typeChoisi.id}">

          <div class="pile">
            <section class="carte">
              <div class="carte-entete"><div><h2>Client</h2><p>Seuls les clients dont le compte est actif sont proposés.</p></div></div>
              <div class="carte-corps">
                <div class="champ ${not empty erreurs.client ? 'a-erreur' : ''}">
                  <label for="client">Client</label>
                  <select class="saisie" id="client" name="client">
                    <option value="">Choisir un client…</option>
                    <c:forEach var="cl" items="${clients}">
                      <option value="${cl.id}" ${cl.id == client ? 'selected' : ''}>
                        <c:out value="${cl.utilisateur.nom} ${cl.utilisateur.prenom} (${cl.utilisateur.email})"/>
                      </option>
                    </c:forEach>
                  </select>
                  <c:if test="${not empty erreurs.client}"><span class="message-erreur"><c:out value="${erreurs.client}"/></span></c:if>
                </div>
              </div>
            </section>

            <section class="carte">
              <div class="carte-entete">
                <div><h2>Garanties</h2><p>Celles que propose ce type d'assurance : au moins une.</p></div>
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
                        <span class="case-detail">Plafond <app:euros valeur="${g.plafond}"/> · franchise <app:euros valeur="${g.franchise}"/></span>
                      </span>
                    </label>
                  </c:forEach>
                </div>
                <c:if test="${not empty erreurs.garanties}"><p class="message-erreur" style="margin-top:10px"><c:out value="${erreurs.garanties}"/></p></c:if>
              </fieldset>
            </section>

            <section class="carte">
              <div class="carte-entete"><div><h2>Période et prime</h2><p>Par défaut, le contrat dure un an.</p></div></div>
              <div class="carte-corps formulaire">
                <div class="champs-2">
                  <div class="champ ${not empty erreurs.dateDebut ? 'a-erreur' : ''}">
                    <label for="dateDebut">Date de début</label>
                    <input class="saisie" type="date" id="dateDebut" name="dateDebut" value="<c:out value='${dateDebut}'/>">
                    <c:if test="${not empty erreurs.dateDebut}"><span class="message-erreur"><c:out value="${erreurs.dateDebut}"/></span></c:if>
                  </div>
                  <div class="champ ${not empty erreurs.dateFin ? 'a-erreur' : ''}">
                    <label for="dateFin">Date de fin</label>
                    <input class="saisie" type="date" id="dateFin" name="dateFin" value="<c:out value='${dateFin}'/>">
                    <c:if test="${not empty erreurs.dateFin}"><span class="message-erreur"><c:out value="${erreurs.dateFin}"/></span></c:if>
                  </div>
                </div>
                <div class="champ ${not empty erreurs.prime ? 'a-erreur' : ''}">
                  <label for="prime">Prime annuelle</label>
                  <div class="saisie-groupe">
                    <input class="saisie" type="text" inputmode="decimal" id="prime" name="prime"
                           value="<c:out value='${prime}'/>" data-minimum="${typeChoisi.prixBase}">
                    <span class="saisie-suffixe">€</span>
                  </div>
                  <span class="aide">Minimum <app:euros valeur="${typeChoisi.prixBase}"/> (prix de base du type d'assurance).</span>
                  <span class="message-avertissement" data-alerte-prime hidden>
                    <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>Prime inférieure au prix de base : le contrat sera refusé.
                  </span>
                  <c:if test="${not empty erreurs.prime}"><span class="message-erreur"><c:out value="${erreurs.prime}"/></span></c:if>
                </div>
              </div>
            </section>
          </div>

          <aside class="carte recap">
            <div class="carte-entete"><h2>Récapitulatif</h2></div>
            <div class="carte-corps">
              <dl>
                <div><dt>Type d'assurance</dt><dd><c:out value="${typeChoisi.nom}"/></dd></div>
                <div class="avec-js"><dt>Client</dt><dd data-recap="client">À choisir</dd></div>
                <div class="avec-js"><dt>Garanties</dt><dd data-recap="garanties">Aucune</dd></div>
                <div class="avec-js"><dt>Période</dt><dd data-recap="periode">À définir</dd></div>
                <div class="recap-total avec-js"><span class="texte-doux">Prime annuelle</span><strong class="nombre" data-recap="prime">—</strong></div>
              </dl>
            </div>
            <div class="carte-pied">
              <button type="submit" class="bouton bouton-principal bouton-bloc">
                <svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Créer le contrat
              </button>
            </div>
          </aside>
        </form>
      </c:if>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
