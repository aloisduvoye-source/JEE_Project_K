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
        <a class="retour" href="${pageContext.request.contextPath}/agent/sinistres">
          <svg class="icone" aria-hidden="true"><use href="#i-fleche-gauche"/></svg>Sinistres
        </a>
        <div class="entete-page">
          <div>
            <h1>Sinistre n° <c:out value="${sinistre.id}"/></h1>
            <p class="sous-titre">
              <app:typeSinistre valeur="${sinistre.typeSinistre}"/> ·
              <c:out value="${sinistre.contrat.client.utilisateur.prenom} ${sinistre.contrat.client.utilisateur.nom}"/>
            </p>
          </div>
        </div>
      </div>

      <%@ include file="/WEB-INF/views/common/messages-sinistre.jspf" %>

      <div class="disposition-laterale">
        <div class="pile">
          <%@ include file="/WEB-INF/views/common/sinistre-infos.jspf" %>

          <section class="carte">
            <div class="carte-entete">
              <div><h2>Garanties du contrat</h2><p>Ce que donnerait l'estimation du client avec chacune : estimation − franchise, dans la limite du plafond.</p></div>
            </div>
            <c:choose>
              <c:when test="${empty propositions}">
                <div class="carte-corps"><p class="texte-doux">Ce contrat n'a aucune garantie : le sinistre ne peut normalement pas être couvert.</p></div>
              </c:when>
              <c:otherwise>
                <div class="tableau-conteneur" style="border:none;border-top:1px solid var(--bordure);border-radius:0">
                  <table class="tableau">
                    <thead>
                      <tr>
                        <th>Garantie</th>
                        <th class="a-droite">Plafond</th>
                        <th class="a-droite">Franchise</th>
                        <th class="a-droite">Avec l'estimation</th>
                      </tr>
                    </thead>
                    <tbody>
                      <c:forEach var="p" items="${propositions}">
                        <tr>
                          <td class="cellule-principale"><c:out value="${p.garantie.nom}"/></td>
                          <td class="a-droite nombre"><app:euros valeur="${p.garantie.plafond}" vide="Aucun"/></td>
                          <td class="a-droite nombre"><app:euros valeur="${p.garantie.franchise}" vide="Aucune"/></td>
                          <td class="a-droite nombre"><app:euros valeur="${p.montant}" vide="—"/></td>
                        </tr>
                      </c:forEach>
                    </tbody>
                  </table>
                </div>
              </c:otherwise>
            </c:choose>
          </section>

          <%@ include file="/WEB-INF/views/common/sinistre-pieces.jspf" %>
          <%@ include file="/WEB-INF/views/common/sinistre-discussion.jspf" %>
        </div>

        <aside class="pile">
          <%-- Actions selon le statut, pour un agent seulement : l'admin consulte.
               ActionSinistreServlet et SinistreService vérifient de toute façon le rôle et le statut. --%>
          <c:if test="${sessionScope.utilisateur.role == 'AGENT'}">
            <c:set var="actionUrl" value="${pageContext.request.contextPath}/agent/sinistres/action"/>
            <c:choose>
              <c:when test="${sinistre.statut == 'DECLARE'}">
                <section class="carte">
                  <div class="carte-entete"><div><h2>Traitement</h2><p>Ce sinistre attend un agent. En le prenant en charge, vous devenez responsable du dossier.</p></div></div>
                  <form class="carte-corps" method="post" action="${actionUrl}">
                    <input type="hidden" name="id" value="${sinistre.id}">
                    <input type="hidden" name="action" value="prendre">
                    <button type="submit" class="bouton bouton-principal bouton-bloc">
                      <svg class="icone" aria-hidden="true"><use href="#i-utilisateur"/></svg>Prendre en charge
                    </button>
                  </form>
                </section>
              </c:when>

              <c:when test="${sinistre.statut == 'EN_ANALYSE'}">
                <section class="carte">
                  <div class="carte-entete"><div><h2>Décision</h2><p>Après examen des pièces, acceptez ou refusez le sinistre.</p></div></div>
                  <form class="carte-corps" method="post" action="${actionUrl}"
                        data-confirmer="Accepter ce sinistre ?"
                        data-confirmer-texte="Il sera couvert par le contrat : vous pourrez ensuite enregistrer l'indemnisation."
                        data-confirmer-bouton="Accepter">
                    <input type="hidden" name="id" value="${sinistre.id}">
                    <input type="hidden" name="action" value="accepter">
                    <button type="submit" class="bouton bouton-principal bouton-bloc">
                      <svg class="icone" aria-hidden="true"><use href="#i-succes"/></svg>Accepter le sinistre
                    </button>
                  </form>
                  <form class="carte-corps formulaire" method="post" action="${actionUrl}" style="border-top:1px solid var(--bordure)"
                        data-confirmer="Refuser ce sinistre ?"
                        data-confirmer-texte="Cette décision est définitive : le dossier sera clos et le motif envoyé à l'assuré."
                        data-confirmer-bouton="Refuser">
                    <input type="hidden" name="id" value="${sinistre.id}">
                    <input type="hidden" name="action" value="refuser">
                    <div class="champ">
                      <label for="motif">Motif du refus</label>
                      <textarea class="saisie" id="motif" name="motif" rows="3" maxlength="1000" required data-compteur
                                placeholder="Garantie non souscrite, exclusion, pièces manquantes…"></textarea>
                      <span class="aide">Visible par l'assuré dans la discussion.</span>
                    </div>
                    <div class="actions-formulaire">
                      <button type="submit" class="bouton bouton-danger-doux bouton-bloc">
                        <svg class="icone" aria-hidden="true"><use href="#i-fermer"/></svg>Refuser le sinistre
                      </button>
                    </div>
                  </form>
                </section>
              </c:when>

              <c:when test="${sinistre.statut == 'ACCEPTE'}">
                <section class="carte">
                  <div class="carte-entete">
                    <div><h2>Indemnisation</h2><p>Choisissez la garantie qui s'applique et le dommage retenu : le montant versé en découle.</p></div>
                  </div>
                  <%-- data-estime : montant estimé par le client, pour prévenir l'agent si le dommage retenu le dépasse --%>
                  <form class="carte-corps formulaire" method="post" action="${actionUrl}" id="formIndemnisation"
                        data-estime="${sinistre.montantEstime}"
                        data-confirmer="Enregistrer l'indemnisation ?" data-confirmer-bouton="Indemniser">
                    <input type="hidden" name="id" value="${sinistre.id}">
                    <input type="hidden" name="action" value="indemniser">
                    <div class="champ">
                      <label for="garantie">Garantie appliquée</label>
                      <select class="saisie" id="garantie" name="garantie" required>
                        <option value="">Choisir une garantie…</option>
                        <c:forEach var="p" items="${propositions}">
                          <option value="${p.garantie.id}" data-franchise="${p.garantie.franchise}" data-plafond="${p.garantie.plafond}">
                            <c:out value="${p.garantie.nom}"/>
                          </option>
                        </c:forEach>
                      </select>
                    </div>
                    <div class="champ">
                      <label for="dommage">Dommage retenu</label>
                      <div class="saisie-groupe">
                        <%-- Prérempli avec l'estimation du client : l'agent la corrige d'après les pièces --%>
                        <input class="saisie" type="text" inputmode="decimal" id="dommage" name="dommage" required
                               value="<c:out value='${sinistre.montantEstime}'/>" placeholder="0,00">
                        <span class="saisie-suffixe">€</span>
                      </div>
                      <span class="aide">Estimation du client : <app:euros valeur="${sinistre.montantEstime}" vide="non précisée"/>.</span>
                      <span class="message-avertissement" data-alerte-estime hidden>
                        <svg class="icone" aria-hidden="true"><use href="#i-alerte"/></svg>Supérieur à l'estimation du client.
                      </span>
                    </div>
                    <dl class="calcul avec-js" data-calcul>
                      <div><dt>Dommage retenu</dt><dd class="nombre" data-calcul="dommage">—</dd></div>
                      <div><dt>Franchise</dt><dd class="nombre" data-calcul="franchise">—</dd></div>
                      <div data-calcul-plafond hidden><dt>Limité au plafond</dt><dd class="nombre" data-calcul="plafond">—</dd></div>
                      <div class="recap-total"><dt>Versé</dt><dd class="nombre" data-calcul="montant">—</dd></div>
                    </dl>
                    <p class="message-erreur" data-calcul-nul hidden>Le dommage ne dépasse pas la franchise : rien à verser. Refusez plutôt le sinistre.</p>
                    <div class="actions-formulaire">
                      <button type="submit" class="bouton bouton-principal bouton-bloc">
                        <svg class="icone" aria-hidden="true"><use href="#i-euro"/></svg>Indemniser l'assuré
                      </button>
                    </div>
                  </form>
                </section>
                <script>
                  // Aperçu du calcul fait par SinistreService.indemniser (qui reste seul juge) : min(dommage - franchise, plafond).
                  // Le texte de la fenêtre de confirmation (app.js, data-confirmer) est complété avant son ouverture.
                  (function () {
                    var formulaire = document.getElementById('formIndemnisation');
                    var garantie = document.getElementById('garantie');
                    var dommage = document.getElementById('dommage');
                    var euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
                    function champ(nom) { return formulaire.querySelector('[data-calcul="' + nom + '"]'); }
                    function montant(texte) {
                      var valeur = parseFloat(String(texte || '').replace(/\s/g, '').replace(',', '.'));
                      return isNaN(valeur) ? null : valeur;
                    }

                    function calculer() {
                      var option = garantie.options[garantie.selectedIndex];
                      var d = montant(dommage.value);
                      var estime = montant(formulaire.dataset.estime);
                      formulaire.querySelector('[data-alerte-estime]').hidden = !(estime !== null && d !== null && d > estime);
                      champ('dommage').textContent = d === null ? '—' : euros.format(d);
                      if (!option.value || d === null) {
                        champ('franchise').textContent = '—';
                        champ('montant').textContent = '—';
                        formulaire.querySelector('[data-calcul-plafond]').hidden = true;
                        formulaire.querySelector('[data-calcul-nul]').hidden = true;
                        return null;
                      }
                      var franchise = montant(option.dataset.franchise) || 0;
                      var plafond = montant(option.dataset.plafond);
                      var resultat = d - franchise;
                      var plafonne = plafond !== null && resultat > plafond;
                      if (plafonne) { resultat = plafond; }
                      resultat = Math.max(resultat, 0);
                      champ('franchise').textContent = '− ' + euros.format(franchise);
                      champ('plafond').textContent = plafond === null ? '—' : euros.format(plafond);
                      formulaire.querySelector('[data-calcul-plafond]').hidden = !plafonne;
                      champ('montant').textContent = euros.format(resultat);
                      formulaire.querySelector('[data-calcul-nul]').hidden = resultat > 0;
                      return resultat;
                    }

                    garantie.addEventListener('change', calculer);
                    dommage.addEventListener('input', calculer);
                    calculer();

                    // Avant la fenêtre de confirmation : montant versé, et avertissement si le dommage dépasse l'estimation
                    formulaire.addEventListener('submit', function () {
                      var resultat = calculer();
                      var d = montant(dommage.value);
                      var estime = montant(formulaire.dataset.estime);
                      formulaire.dataset.confirmer = resultat === null
                          ? 'Enregistrer l\'indemnisation ?'
                          : 'Verser ' + euros.format(resultat) + ' à l\'assuré ?';
                      formulaire.dataset.confirmerTexte = (estime !== null && d !== null && d > estime
                          ? 'Le dommage retenu (' + euros.format(d) + ') dépasse l\'estimation du client (' + euros.format(estime) + '). '
                          : '') + 'Cette action est définitive : le dossier sera clos.';
                    });
                  })();
                </script>
              </c:when>
            </c:choose>
          </c:if>

          <%@ include file="/WEB-INF/views/common/sinistre-suivi.jspf" %>
        </aside>
      </div>
    </div>
  </main>

  <%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
