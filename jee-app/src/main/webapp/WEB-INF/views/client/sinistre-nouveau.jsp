<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="app" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <title>Déclarer un sinistre · Billy Assurance</title>
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
            <h1>Déclarer un sinistre</h1>
            <p class="sous-titre">Décrivez ce qui s'est passé : un agent analysera votre dossier.</p>
          </div>
        </div>
      </div>

      <div class="alerte ton-attention" role="note" data-permanente>
        <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>
        <div class="alerte-texte">
          <strong>En cas d'urgence</strong> : personne blessée ou danger immédiat, appelez d'abord les secours (15, 17, 18 ou 112).
        </div>
      </div>

      <c:if test="${not empty erreurs.global}">
        <div class="alerte ton-danger" role="alert">
          <svg class="icone" aria-hidden="true"><use href="#i-erreur"/></svg>
          <div class="alerte-texte"><c:out value="${erreurs.global}"/></div>
        </div>
      </c:if>

      <c:choose>
        <c:when test="${empty contrats}">
          <div class="carte vide">
            <span class="pastille"><svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg></span>
            <h2>Aucun contrat ne peut couvrir un sinistre</h2>
            <p>Un sinistre se déclare sur un contrat payé et déjà commencé. Payez votre contrat depuis sa fiche pour pouvoir déclarer un sinistre.</p>
            <a class="bouton bouton-principal" href="${pageContext.request.contextPath}/client/contrats">
              <svg class="icone" aria-hidden="true"><use href="#i-contrat"/></svg>Voir mes contrats
            </a>
          </div>
        </c:when>
        <c:otherwise>
          <form class="disposition-laterale form-pieces" method="post" enctype="multipart/form-data"
                action="${pageContext.request.contextPath}/client/sinistres/nouveau" novalidate>
            <div class="pile">
              <section class="carte">
                <div class="carte-entete"><div><h2>Contrat et date</h2><p>Le contrat doit couvrir la date du sinistre.</p></div></div>
                <div class="carte-corps formulaire">
                  <div class="champ ${not empty erreurs.contrat ? 'a-erreur' : ''}">
                    <label for="contrat">Contrat concerné</label>
                    <select class="saisie" id="contrat" name="contrat" required>
                      <option value="">Choisir un contrat…</option>
                      <c:forEach var="ct" items="${contrats}">
                        <option value="${ct.id}" ${ct.id == contrat ? 'selected' : ''}>
                          <c:out value="${ct.typeAssurance.nom} · n° ${ct.id}${ct.statut == 'ACTIF' ? '' : (ct.statut == 'EXPIRE' ? ' (expiré)' : (ct.statut == 'RESILIE' ? ' (résilié)' : ''))}"/>
                        </option>
                      </c:forEach>
                    </select>
                    <span class="aide">Un contrat expiré ou résilié reste proposé pour un sinistre survenu pendant qu'il vous couvrait.</span>
                    <c:if test="${not empty erreurs.contrat}"><span class="message-erreur"><c:out value="${erreurs.contrat}"/></span></c:if>
                  </div>
                  <div class="champ ${not empty erreurs.dateSinistre ? 'a-erreur' : ''}">
                    <label for="dateSinistre">Date du sinistre</label>
                    <%-- max : un sinistre ne peut pas être déclaré à l'avance (vérifié aussi par le service) --%>
                    <input class="saisie" type="date" id="dateSinistre" name="dateSinistre" max="${aujourdhui}" required
                           value="<c:out value='${dateSinistre}'/>">
                    <c:if test="${not empty erreurs.dateSinistre}"><span class="message-erreur"><c:out value="${erreurs.dateSinistre}"/></span></c:if>
                  </div>
                </div>
              </section>

              <section class="carte">
                <div class="carte-entete"><div><h2>Ce qui s'est passé</h2><p>Plus la description est précise, plus le traitement est rapide.</p></div></div>
                <div class="carte-corps formulaire">
                  <fieldset class="champ ${not empty erreurs.type ? 'a-erreur' : ''}">
                    <legend class="libelle">Type de sinistre</legend>
                    <div class="choix-cases" style="grid-template-columns:repeat(auto-fit, minmax(200px, 1fr))">
                      <c:forEach var="t" items="${types}">
                        <label class="case-carte" for="type-${t}">
                          <input type="radio" id="type-${t}" name="type" value="${t}" required ${t == type ? 'checked' : ''}>
                          <span class="case-titre"><app:typeSinistre valeur="${t}"/></span>
                        </label>
                      </c:forEach>
                    </div>
                    <c:if test="${not empty erreurs.type}"><span class="message-erreur"><c:out value="${erreurs.type}"/></span></c:if>
                  </fieldset>
                  <div class="champ ${not empty erreurs.description ? 'a-erreur' : ''}">
                    <label for="description">Circonstances</label>
                    <textarea class="saisie" id="description" name="description" rows="6" maxlength="${longueurMax}" required data-compteur
                              placeholder="Où, quand, comment ? Personnes et biens concernés, témoins…"><c:out value="${description}"/></textarea>
                    <c:if test="${not empty erreurs.description}"><span class="message-erreur"><c:out value="${erreurs.description}"/></span></c:if>
                  </div>
                  <div class="champ ${not empty erreurs.montantEstime ? 'a-erreur' : ''}">
                    <label for="montantEstime">Montant estimé des dommages <span class="texte-doux">(facultatif)</span></label>
                    <div class="saisie-groupe">
                      <input class="saisie" type="text" inputmode="decimal" id="montantEstime" name="montantEstime"
                             value="<c:out value='${montantEstime}'/>" placeholder="0,00">
                      <span class="saisie-suffixe">€</span>
                    </div>
                    <span class="aide">Une estimation suffit : l'agent fixe le dommage retenu d'après vos justificatifs.</span>
                    <c:if test="${not empty erreurs.montantEstime}"><span class="message-erreur"><c:out value="${erreurs.montantEstime}"/></span></c:if>
                  </div>
                </div>
              </section>

              <section class="carte">
                <div class="carte-entete"><div><h2>Pièces justificatives</h2><p>Facultatif : vous pourrez aussi les ajouter plus tard, depuis la fiche du sinistre.</p></div></div>
                <div class="carte-corps formulaire">
                  <div class="champ">
                    <label for="pieces">Fichiers</label>
                    <input class="saisie saisie-fichier" type="file" id="pieces" name="pieces" multiple
                           accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png">
                    <span class="aide">PDF, JPG ou PNG, 5 Mo maximum par fichier.</span>
                    <c:if test="${not empty erreurs}">
                      <span class="message-avertissement"><svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>Si vous aviez choisi des fichiers, choisissez-les à nouveau.</span>
                    </c:if>
                  </div>
                </div>
              </section>
            </div>

            <aside class="pile">
              <section class="carte">
                <div class="carte-entete"><div><h2>Documents à préparer</h2><p>Un agent pourra vous les demander.</p></div></div>
                <ul class="liste-lignes">
                  <li><div><strong>Tous les sinistres</strong><div class="detail">Photos des dommages, factures ou devis de réparation.</div></div></li>
                  <li><div><strong>Accident</strong><div class="detail">Constat amiable, permis, carte grise, coordonnées des témoins.</div></div></li>
                  <li><div><strong>Vol</strong><div class="detail">Récépissé du dépôt de plainte (obligatoire), factures des objets volés.</div></div></li>
                  <li><div><strong>Incendie</strong><div class="detail">Rapport des pompiers s'il y en a un, factures des biens détruits.</div></div></li>
                  <li><div><strong>Dégâts des eaux</strong><div class="detail">Constat amiable de dégât des eaux, facture du plombier.</div></div></li>
                  <li><div><strong>Maladie</strong><div class="detail">Ordonnances, feuilles de soins, décompte de la Sécurité sociale.</div></div></li>
                </ul>
                <div class="carte-pied">
                  <a class="bouton bouton-fantome" href="${pageContext.request.contextPath}/client/sinistres">Annuler</a>
                  <button type="submit" class="bouton bouton-principal">
                    <svg class="icone" aria-hidden="true"><use href="#i-coche"/></svg>Déclarer
                  </button>
                </div>
              </section>
            </aside>
          </form>
          <%@ include file="/WEB-INF/views/common/pieces-taille.jspf" %>
        </c:otherwise>
      </c:choose>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
