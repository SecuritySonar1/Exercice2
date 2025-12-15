package com.ipiecoles.java.java350.service;

import com.ipiecoles.java.java350.exception.EmployeException;
import com.ipiecoles.java.java350.model.Employe;
import com.ipiecoles.java.java350.model.Entreprise;
import com.ipiecoles.java.java350.model.NiveauEtude;
import com.ipiecoles.java.java350.model.Poste;
import com.ipiecoles.java.java350.repository.EmployeRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.time.LocalDate;

@Service
public class EmployeService {

    private final EmployeRepository employeRepository;

    public EmployeService(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    /**
     * Méthode enregistrant un nouvel employé dans l'entreprise
     */
    public void embaucheEmploye(String nom, String prenom, Poste poste, NiveauEtude niveauEtude, Double tempsPartiel) 
            throws EmployeException, EntityExistsException {

        // Type d'employé à partir du poste
        String typeEmploye = poste.name().substring(0,1);

        // Récupération du dernier matricule et incrémentation
        String lastMatricule = employeRepository.findLastMatricule();
        if(lastMatricule == null){
            lastMatricule = Entreprise.MATRICULE_INITIAL;
        }
        Integer numeroMatricule = Integer.parseInt(lastMatricule) + 1;
        if(numeroMatricule >= 100000){
            throw new EmployeException("Limite des 100000 matricules atteinte !");
        }

        // Création du matricule complet
        String matricule = "00000" + numeroMatricule;
        matricule = typeEmploye + matricule.substring(matricule.length() - 5);

        // Vérification de l'existence
        if(employeRepository.findByMatricule(matricule) != null){
            throw new EntityExistsException("L'employé de matricule " + matricule + " existe déjà en BDD");
        }

        // Calcul du salaire
        Double salaire = Entreprise.COEFF_SALAIRE_ETUDES.get(niveauEtude) * Entreprise.SALAIRE_BASE;
        if(tempsPartiel != null){
            salaire *= tempsPartiel;
        }
        salaire = Math.round(salaire*100d)/100d;

        // Création et sauvegarde
        Employe employe = new Employe(nom, prenom, matricule, LocalDate.now(), salaire, Entreprise.PERFORMANCE_BASE, tempsPartiel);
        employeRepository.save(employe);
    }

    /**
     * Calcul de la performance d'un commercial
     */
    public void calculPerformanceCommercial(String matricule, Long caTraite, Long objectifCa) throws EmployeException {
        // Vérification des paramètres
        if(caTraite == null || caTraite < 0){
            throw new EmployeException("Le chiffre d'affaire traité ne peut être négatif ou null !");
        }
        if(objectifCa == null || objectifCa < 0){
            throw new EmployeException("L'objectif de chiffre d'affaire ne peut être négatif ou null !");
        }
        if(matricule == null || !matricule.startsWith("C")){
            throw new EmployeException("Le matricule ne peut être null et doit commencer par un C !");
        }

        // Récupération du commercial
        Employe employe = getCommercial(matricule);

        // Calcul de la performance selon le CA
        int performance = calculerPerformance(employe, caTraite, objectifCa);

        // Bonus si meilleur que la moyenne
        if(isMeilleurQueMoyenne(performance)){
            performance++;
        }

        // Affectation et sauvegarde
        employe.setPerformance(performance);
        employeRepository.save(employe);
    }

    /* =================== Méthodes privées auxiliaires =================== */

    private Employe getCommercial(String matricule) throws EmployeException {
        Employe employe = employeRepository.findByMatricule(matricule);
        if (employe == null) {
            throw new EmployeException("Le matricule " + matricule + " n'existe pas !");
        }
        return employe;
    }

    private int calculerPerformance(Employe employe, Long caTraite, Long objectifCa) {
        int performance = Entreprise.PERFORMANCE_BASE;

        if(caTraite < objectifCa * 0.8) return performance;
        if(caTraite < objectifCa * 0.95) return Math.max(Entreprise.PERFORMANCE_BASE, employe.getPerformance() - 2);
        if(caTraite <= objectifCa * 1.05) return Math.max(Entreprise.PERFORMANCE_BASE, employe.getPerformance());
        if(caTraite <= objectifCa * 1.2) return employe.getPerformance() + 1;

        return employe.getPerformance() + 4;
    }

    private boolean isMeilleurQueMoyenne(Integer performance){
        Double moyenne = employeRepository.avgPerformanceWhereMatriculeStartsWith("C");
        return moyenne != null && performance > moyenne;
    }
}
