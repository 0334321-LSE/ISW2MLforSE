package isw2_data_retriever.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDate;
import java.util.*;

public class BugTicket {

    public BugTicket(){

    }

    public BugTicket(String issueKey, LocalDate ticketsCreationDate, LocalDate ticketsResolutionDate, Version injectedVersion){
        this.issueKeys = issueKey;
        this.ticketsCreationDate = ticketsCreationDate;
        this.ticketsResolutionDate = ticketsResolutionDate;
        this.injectedVersion = injectedVersion;
    }

    private  String issueKeys;
    private  LocalDate ticketsCreationDate;
    private  LocalDate ticketsResolutionDate;
    private Version injectedVersion;

    private Version openingVersion;

    private Version fixedVersion;
    private ArrayList<RevCommit> associatedCommit;

    private RevCommit lastCommit;

    public RevCommit getLastCommit() {
        return lastCommit;
    }

    public void setTicketsCreationDate(LocalDate ticketsCreationDate) {
        this.ticketsCreationDate = ticketsCreationDate;
    }

    public void setInjectedVersion(Version injectedVersion){
        this.injectedVersion= injectedVersion;
    }
    public void setOpeningVersion(Version openingVersion){
        this.openingVersion= openingVersion;
    }
    public void setFixedVersion(Version fixedVersion){
        this.fixedVersion= fixedVersion;
    }

    public void setAssociatedCommit(ArrayList<RevCommit> associatedCommit) {
        this.associatedCommit = associatedCommit;
        this.lastCommit = getLastCommit(associatedCommit);
    }

    /** Get last commit from one commit list */
    private static RevCommit getLastCommit(List<RevCommit> commitsList) {
        if(commitsList.size()==0)
            return null;
        RevCommit lastCommit = commitsList.get(0);
        for(RevCommit commit : commitsList) {
            //if commitDate > lastCommitDate then refresh lastCommit
            if(commit.getCommitterIdent().getWhen().after(lastCommit.getCommitterIdent().getWhen())) {
                lastCommit = commit;

            }
        }
        return lastCommit;

    }
    public void setVersionInfo(List<BugTicket> bugTickets, List<Version> versionList){

        for (BugTicket bugTicket : bugTickets) {

            bugTicket.setOpeningVersion(bugTicket.getOvFromCreationDate(versionList));
            bugTicket.setFixedVersion(bugTicket.getFvFromResolutionDate(versionList));
            bugTicket.setInjectedVersion(bugTicket.getInjectedVersion());

        }

    }

    /** Return the ticket correct opening version from opening date*/
    private Version getOvFromCreationDate(List<Version> versionList){
        int i =0, flag =0;
        Version openingVersion = new Version();

        for (i=0; i< versionList.size() && flag==0; i++){
            if (this.getTicketsCreationDate().isBefore(versionList.get(i).getVersionDate())) {
                flag = 1;
                openingVersion = versionList.get(i);
            }
        }
        if(flag == 0){
            //if it comes here there isn't a valid OV, it may happen with not closed project
            //add the first version ( the NULL one )
            openingVersion = versionList.get(0);
        }

        return openingVersion;
    }

    /** Return the ticket correct fixed version from resolution date*/
    private Version getFvFromResolutionDate(List<Version> versionList){
        int i, flag =0;
        Version fixedVersion =  new Version();
        for (i=0; i< versionList.size() && flag==0; i++){
            if (this.getTicketsResolutionDate().isBefore(versionList.get(i).getVersionDate())){
                flag=1;
                fixedVersion = versionList.get(i);
            }
        }
        if(flag == 0){
            //if it comes here there isn't a valid FV, it may happen with not closed project
            //add the first version ( the NULL one )
            fixedVersion = versionList.get(0);
        }
        return fixedVersion;
    }

    public ArrayList<RevCommit> getAssociatedCommit() {
        return associatedCommit;
    }

    private void printVersionInformation(){
        System.out.println("\n---------------------------------------------------------------------------");
        System.out.println("TICKET: "+this.issueKeys);
        System.out.print("| Injected Version: "+this.injectedVersion);
        System.out.print(" | Opening Version: "+this.openingVersion);
        System.out.print(" | Fixed Version: "+this.fixedVersion+" |");
    }

    public void printVersionInformationList(List<BugTicket> ticketVersionInformationList){
        for ( BugTicket ticket: ticketVersionInformationList)
            ticket.printVersionInformation();

    }

    public String getIssueKey(){
        return this.issueKeys;
    }
    public LocalDate getTicketsCreationDate(){
        return this.ticketsCreationDate;
    }
    public LocalDate getTicketsResolutionDate(){
        return this.ticketsResolutionDate;
    }

    public Version getOpeningVersion(){ return this.openingVersion;}

    public Version getFixedVersion(){ return this.fixedVersion;}

    public Version getInjectedVersion(){ return this.injectedVersion;}

}