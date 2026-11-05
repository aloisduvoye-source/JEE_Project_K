/*
 * Billy Assurance : comportements communs à toutes les pages.
 * Chargé par /WEB-INF/views/common/head.jspf (attribut defer : exécuté une fois la page lue).
 *
 * Amélioration progressive : chaque page fonctionne sans JavaScript (formulaires, liens),
 * ce script ajoute seulement du confort. Chaque fonction ne s'active que si la page contient
 * les éléments concernés (attributs data-...).
 *
 *   data-basculer-theme      bouton thème clair / sombre
 *   data-menu-bouton         bouton ☰ du menu sur mobile
 *   data-nav                 navigation : le lien de la page courante est mis en évidence
 *   .alerte                  bouton de fermeture ; data-nettoyer-url="p1 p2" retire ces paramètres de l'adresse
 *   data-confirmer           formulaire à confirmer dans une fenêtre avant l'envoi
 *   data-filtres="#cible"    recherche et filtres d'une liste (éléments data-filtre-element)
 *   data-voir-mdp            bouton afficher / masquer un mot de passe
 *   data-force               jauge de solidité d'un nouveau mot de passe
 *   data-identique="#id"     vérifie que la confirmation correspond au mot de passe
 *   data-compteur            compteur de caractères d'un textarea (maxlength)
 *   data-periode             barre de progression de la période d'un contrat
 *   #formulaire-contrat      dates, prime minimale et récapitulatif du nouveau contrat
 */
(function () {
  'use strict';

  var racine = document.documentElement;
  var euros = new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' });
  var dateLongue = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });

  function tous(selecteur, parent) {
    return Array.prototype.slice.call((parent || document).querySelectorAll(selecteur));
  }

  // "2026-11-05" -> Date locale (sans décalage de fuseau horaire), ou null
  function lireDate(texte) {
    var morceaux = /^(\d{4})-(\d{2})-(\d{2})$/.exec(texte || '');
    return morceaux ? new Date(+morceaux[1], +morceaux[2] - 1, +morceaux[3]) : null;
  }

  function ecrireDate(date) {
    var mois = String(date.getMonth() + 1).padStart(2, '0');
    var jour = String(date.getDate()).padStart(2, '0');
    return date.getFullYear() + '-' + mois + '-' + jour;
  }

  // "1 250,50" ou "1250.5" -> 1250.5, ou NaN
  function lireMontant(texte) {
    return parseFloat(String(texte || '').replace(/\s/g, '').replace(',', '.'));
  }

  // Comparaison sans accents ni majuscules, pour la recherche
  function normaliser(texte) {
    return texte.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }

  function icone(nom) {
    return '<svg class="icone" aria-hidden="true"><use href="#i-' + nom + '"/></svg>';
  }

  // --- Thème clair / sombre (le choix est retenu par le navigateur) ---------

  tous('[data-basculer-theme]').forEach(function (bouton) {
    bouton.addEventListener('click', function () {
      var theme = racine.dataset.theme === 'dark' ? 'light' : 'dark';
      racine.dataset.theme = theme;
      try { localStorage.setItem('theme', theme); } catch (e) { /* stockage indisponible */ }
    });
  });

  // --- Menu mobile -----------------------------------------------------------

  var boutonMenu = document.querySelector('[data-menu-bouton]');
  var menu = document.getElementById('menu-principal');
  if (boutonMenu && menu) {
    boutonMenu.addEventListener('click', function () {
      var ouvert = menu.classList.toggle('ouvert');
      boutonMenu.setAttribute('aria-expanded', String(ouvert));
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && menu.classList.contains('ouvert')) {
        menu.classList.remove('ouvert');
        boutonMenu.setAttribute('aria-expanded', 'false');
        boutonMenu.focus();
      }
    });
  }

  // --- Lien de la page courante dans la navigation ---------------------------
  // Le lien dont l'adresse correspond le mieux (le plus long préfixe) à la page affichée

  tous('[data-nav]').forEach(function (nav) {
    var chemin = location.pathname.replace(/\/$/, '') || '/';
    var meilleur = null;
    var longueur = -1;
    tous('a[href]', nav).forEach(function (lien) {
      var cible = new URL(lien.href, location.href).pathname.replace(/\/$/, '') || '/';
      var correspond = chemin === cible || (cible !== '/' && chemin.indexOf(cible + '/') === 0);
      if (correspond && cible.length > longueur) {
        meilleur = lien;
        longueur = cible.length;
      }
    });
    if (meilleur) {
      meilleur.setAttribute('aria-current', 'page');
    }
  });

  // --- Alertes ---------------------------------------------------------------

  function fermerAlerte(alerte) {
    alerte.classList.add('disparait');
    setTimeout(function () { alerte.remove(); }, 300);
  }

  tous('.alerte').forEach(function (alerte) {
    var bouton = document.createElement('button');
    bouton.type = 'button';
    bouton.className = 'alerte-fermer';
    bouton.setAttribute('aria-label', 'Fermer le message');
    bouton.innerHTML = icone('fermer');
    bouton.addEventListener('click', function () { fermerAlerte(alerte); });
    alerte.appendChild(bouton);

    // Message de confirmation (?cree=..., ?succes=...) : retiré de l'adresse pour qu'un
    // rechargement de la page ne l'affiche pas une deuxième fois
    if (alerte.dataset.nettoyerUrl && window.history.replaceState) {
      var url = new URL(location.href);
      alerte.dataset.nettoyerUrl.split(' ').forEach(function (parametre) { url.searchParams.delete(parametre); });
      history.replaceState(null, '', url.pathname + url.search + url.hash);
    }
  });

  // --- Confirmation avant l'envoi d'un formulaire ----------------------------
  // <form data-confirmer="Titre" data-confirmer-texte="Détail" data-confirmer-bouton="Supprimer">

  var modale = null;
  var formulaireAConfirmer = null;

  function creerModale() {
    modale = document.createElement('dialog');
    modale.className = 'modale';
    modale.innerHTML =
      '<form method="dialog">' +
      '  <div class="pastille ton-danger">' + icone('alerte') + '</div>' +
      '  <h2 data-modale-titre></h2>' +
      '  <p data-modale-texte></p>' +
      '  <div class="modale-actions">' +
      '    <button class="bouton" value="annuler">Annuler</button>' +
      '    <button class="bouton bouton-danger" value="ok" data-modale-bouton></button>' +
      '  </div>' +
      '</form>';
    document.body.appendChild(modale);
    modale.addEventListener('close', function () {
      if (modale.returnValue === 'ok' && formulaireAConfirmer) {
        formulaireAConfirmer.submit();   // submit() n'est pas intercepté de nouveau
      }
      formulaireAConfirmer = null;
    });
  }

  document.addEventListener('submit', function (e) {
    var formulaire = e.target;
    if (!formulaire.matches('form[data-confirmer]')) {
      return;
    }
    e.preventDefault();
    var titre = formulaire.dataset.confirmer;
    if (typeof HTMLDialogElement !== 'function') {
      if (window.confirm(titre)) { formulaire.submit(); }
      return;
    }
    if (!modale) { creerModale(); }
    formulaireAConfirmer = formulaire;
    modale.querySelector('[data-modale-titre]').textContent = titre;
    modale.querySelector('[data-modale-texte]').textContent = formulaire.dataset.confirmerTexte || '';
    modale.querySelector('[data-modale-bouton]').textContent = formulaire.dataset.confirmerBouton || 'Confirmer';
    modale.returnValue = '';
    modale.showModal();
    modale.querySelector('[value="annuler"]').focus();
  });

  // --- Recherche et filtres d'une liste --------------------------------------
  // <div data-filtres="#liste"> contient un champ data-filtre-texte et des boutons data-filtre-valeur.
  // Dans #liste : les éléments data-filtre-element (avec data-filtre-cle) et un message data-filtre-vide.

  tous('[data-filtres]').forEach(function (zone) {
    var liste = document.querySelector(zone.dataset.filtres);
    if (!liste) { return; }
    var elements = tous('[data-filtre-element]', liste);
    var champ = zone.querySelector('[data-filtre-texte]');
    var boutons = tous('[data-filtre-valeur]', zone);
    var compteur = zone.querySelector('[data-filtre-compteur]');
    var messageVide = liste.querySelector('[data-filtre-vide]');
    var valeurChoisie = '';

    function appliquer() {
      var texte = champ ? normaliser(champ.value.trim()) : '';
      var visibles = 0;
      elements.forEach(function (element) {
        var cleOk = !valeurChoisie || element.dataset.filtreCle === valeurChoisie;
        var texteOk = !texte || normaliser(element.textContent).indexOf(texte) !== -1;
        element.hidden = !(cleOk && texteOk);
        if (!element.hidden) { visibles++; }
      });
      if (compteur) {
        compteur.textContent = visibles + ' sur ' + elements.length;
      }
      if (messageVide) {
        messageVide.hidden = visibles !== 0;
      }
    }

    if (champ) { champ.addEventListener('input', appliquer); }
    boutons.forEach(function (bouton) {
      bouton.addEventListener('click', function () {
        valeurChoisie = bouton.dataset.filtreValeur;
        boutons.forEach(function (b) { b.setAttribute('aria-pressed', String(b === bouton)); });
        appliquer();
      });
    });
    appliquer();
  });

  // --- Mots de passe ---------------------------------------------------------

  tous('[data-voir-mdp]').forEach(function (bouton) {
    var champ = bouton.parentElement.querySelector('input');
    bouton.addEventListener('click', function () {
      var visible = champ.type === 'password';
      champ.type = visible ? 'text' : 'password';
      bouton.setAttribute('aria-pressed', String(visible));
      bouton.setAttribute('aria-label', visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
      bouton.innerHTML = icone(visible ? 'oeil-barre' : 'oeil');
      champ.focus();
    });
  });

  // Jauge de solidité : la règle du serveur (8 caractères minimum) reste la seule obligatoire
  tous('input[data-force]').forEach(function (champ) {
    var zone = document.createElement('div');
    zone.className = 'aide-ligne';
    zone.innerHTML = '<div class="force" style="flex:1;align-self:center"><span></span><span></span><span></span><span></span></div>' +
                     '<span class="aide" data-force-libelle>8 caractères minimum</span>';
    champ.closest('.champ').appendChild(zone);
    var jauge = zone.querySelector('.force');
    var libelle = zone.querySelector('[data-force-libelle]');

    champ.addEventListener('input', function () {
      var mdp = champ.value;
      var niveau = 0;
      var texte = '8 caractères minimum';
      if (mdp.length >= 8) {
        niveau = 1;
        if (/[a-z]/.test(mdp) && /[A-Z]/.test(mdp)) { niveau++; }
        if (/\d/.test(mdp)) { niveau++; }
        if (/[^A-Za-z0-9]/.test(mdp) || mdp.length >= 14) { niveau++; }
        texte = ['', 'Faible', 'Moyen', 'Bon', 'Excellent'][Math.min(niveau, 4)];
      } else if (mdp.length > 0) {
        texte = 'Trop court (' + mdp.length + '/8)';
      }
      jauge.dataset.niveau = String(Math.min(niveau, 4));
      libelle.textContent = texte;
    });
  });

  tous('input[data-identique]').forEach(function (confirmation) {
    var original = document.querySelector(confirmation.dataset.identique);
    if (!original) { return; }
    var message = document.createElement('span');
    message.className = 'aide';
    confirmation.closest('.champ').appendChild(message);

    function verifier() {
      if (!confirmation.value) {
        message.textContent = '';
        return;
      }
      var ok = confirmation.value === original.value;
      message.className = 'aide ' + (ok ? 'correspondance-ok' : 'correspondance-ko');
      message.textContent = ok ? '✓ Les mots de passe correspondent' : 'Les mots de passe ne correspondent pas';
    }
    confirmation.addEventListener('input', verifier);
    original.addEventListener('input', verifier);
  });

  // --- Compteur de caractères ------------------------------------------------

  tous('textarea[data-compteur][maxlength]').forEach(function (zone) {
    var compteur = document.createElement('span');
    compteur.className = 'aide';
    compteur.style.textAlign = 'right';
    zone.closest('.champ').appendChild(compteur);
    function majCompteur() {
      compteur.textContent = zone.value.length + ' / ' + zone.maxLength;
    }
    zone.addEventListener('input', majCompteur);
    majCompteur();
  });

  // --- Période d'un contrat : part déjà écoulée ------------------------------
  // <div data-periode data-debut="2026-01-01" data-fin="2026-12-31">

  tous('[data-periode]').forEach(function (zone) {
    var debut = lireDate(zone.dataset.debut);
    var fin = lireDate(zone.dataset.fin);
    if (!debut || !fin || fin <= debut) { zone.hidden = true; return; }
    var aujourdhui = new Date();
    var part = Math.max(0, Math.min(1, (aujourdhui - debut) / (fin - debut)));
    var jours = Math.ceil((fin - aujourdhui) / 86400000);
    var texte = aujourdhui < debut ? 'Commence le ' + dateLongue.format(debut)
              : jours > 0 ? jours + ' jour' + (jours > 1 ? 's' : '') + ' restant' + (jours > 1 ? 's' : '')
              : 'Période terminée';
    zone.querySelector('[data-periode-texte]').textContent = texte;
    zone.querySelector('[data-periode-part]').textContent = Math.round(part * 100) + ' %';
    var barre = zone.querySelector('.progression span');
    requestAnimationFrame(function () { barre.style.width = (part * 100) + '%'; });
  });

  // --- Nouveau contrat -------------------------------------------------------

  var formContrat = document.getElementById('formulaire-contrat');
  if (formContrat) {
    var champDebut = formContrat.querySelector('#dateDebut');
    var champFin = formContrat.querySelector('#dateFin');
    var champPrime = formContrat.querySelector('#prime');
    var champClient = formContrat.querySelector('#client');
    var cases = tous('input[name="garanties"]', formContrat);
    var primeMinimum = lireMontant(champPrime.dataset.minimum);
    var alertePrime = formContrat.querySelector('[data-alerte-prime]');
    var finSaisieALaMain = Boolean(champFin.value);

    // Un an moins un jour après la date de début : du 05/11/2026 au 04/11/2027
    function finParDefaut() {
      var debut = lireDate(champDebut.value);
      if (!debut || finSaisieALaMain) { return; }
      var fin = new Date(debut.getFullYear() + 1, debut.getMonth(), debut.getDate() - 1);
      champFin.value = ecrireDate(fin);
    }

    function majRecap() {
      var cochees = cases.filter(function (c) { return c.checked; }).length;
      var recapGaranties = document.querySelector('[data-recap="garanties"]');
      var recapClient = document.querySelector('[data-recap="client"]');
      var recapPeriode = document.querySelector('[data-recap="periode"]');
      var recapPrime = document.querySelector('[data-recap="prime"]');

      if (recapGaranties) {
        recapGaranties.textContent = cochees === 0 ? 'Aucune' : cochees + ' sur ' + cases.length;
      }
      if (recapClient) {
        recapClient.textContent = champClient.value ? champClient.options[champClient.selectedIndex].text.trim() : 'À choisir';
      }
      if (recapPeriode) {
        var debut = lireDate(champDebut.value);
        var fin = lireDate(champFin.value);
        recapPeriode.textContent = debut && fin ? dateLongue.format(debut) + ' → ' + dateLongue.format(fin) : 'À définir';
      }
      var prime = lireMontant(champPrime.value);
      if (recapPrime) {
        recapPrime.textContent = isNaN(prime) ? '—' : euros.format(prime);
      }
      // Avertissement seulement : le serveur refuse de toute façon une prime trop basse
      var tropBasse = !isNaN(prime) && !isNaN(primeMinimum) && prime < primeMinimum;
      champPrime.closest('.champ').classList.toggle('a-avertissement', tropBasse);
      alertePrime.hidden = !tropBasse;
    }

    if (!champDebut.value) {
      champDebut.value = ecrireDate(new Date());
    }
    finParDefaut();

    champDebut.addEventListener('change', function () { finParDefaut(); majRecap(); });
    champFin.addEventListener('change', function () { finSaisieALaMain = Boolean(champFin.value); majRecap(); });
    champPrime.addEventListener('input', majRecap);
    champClient.addEventListener('change', majRecap);
    cases.forEach(function (c) { c.addEventListener('change', majRecap); });

    var toutCocher = formContrat.querySelector('[data-tout-cocher]');
    if (toutCocher) {
      toutCocher.addEventListener('click', function () {
        var cocher = cases.some(function (c) { return !c.checked; });
        cases.forEach(function (c) { c.checked = cocher; });
        toutCocher.textContent = cocher ? 'Tout désélectionner' : 'Tout sélectionner';
        majRecap();
      });
    }
    majRecap();
  }
})();
