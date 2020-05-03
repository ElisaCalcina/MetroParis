package it.polito.tdp.metroparis.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {
	
	//costruisco il grafo
	private Graph<Fermata, DefaultEdge> graph; //defaultEdge= grafo non pesato
	private List<Fermata> fermate;
	private Map<Integer, Fermata> fermateIdMap; //per il SECONDO MODO
	
	public Model() { //se costruzione grafo lunga conviene creare un nuovo metodo nel model e non farlo nel costruttore
		this.graph= new SimpleDirectedGraph<>(DefaultEdge.class);
		
		//ORA AGGIUNGO VERTICI

		MetroDAO dao= new MetroDAO();
		this.fermate=dao.getAllFermate();  //lista che contiene tutte le fermate, ciascuna è un vertice del grafo
		
		//versione accelerata per passare da id a fermata--> modo 2
		this.fermateIdMap=new HashMap<Integer, Fermata>();
		for(Fermata f: fermate) {
			fermateIdMap.put(f.getIdFermata(), f); //mappa  che mappa id fermata sull'oggetto fermata, ottengo ogg fermata da id senza passare dal db
		}
		
		Graphs.addAllVertices(this.graph, this.fermate);
		
	//	System.out.println(graph);
		
		//ORA AGGIUNGO ARCHI
		//PRIMO MODO= chiedo al grafo se per ogni coppia di vertici c'è o no un arco (coppie di vertici)
		//se grafo piccolo questo metodo va bene, se ho molti vertici (da 100 in più) richiede un certo tempo di esecuzione
	/*	for(Fermata fp: this.fermate) {
			for(Fermata fa: this.fermate) {
				if(dao.fermateConnesse(fp, fa)) { 	//esiste una connessione che va da fa e fp
													//date due fermate dimmi se esiste una connessione che fa tra fa e fp
					this.graph.addEdge(fp, fa);
				}
			}
		}*/
	
		//SECONDO MODO=da un vertice, trova tutti i connessi
		//se grado medio vertici basso rispetto al numero di vertici (se densità è bassa), allora va bene
	/*	for(Fermata fp: fermate) {
			List<Fermata> connesse = dao.fermateSuccessive(fp, fermateIdMap);//tutte le fermate adiacenti a fp;
			
			//aggiungo arco per ciascun elemento della lista
			for(Fermata fa: connesse) {
				this.graph.addEdge(fp, fa);
			}
		}
		*/
	
	
		//TERZO MODO=ci facciamo dare dal database gli archi che ci servono
		//se database fatto bene e ha tutti i dati questo è il metodo migliore di tutti
		//se però archi devono chiedere al database di fare query più complicate, le query possono essere poco efficienti ed è meglio usare il metodo 2
		List<CoppiaFermate> coppie = dao.coppieFermate(fermateIdMap);
		for(CoppiaFermate c: coppie) {
			graph.addEdge(c.getFa(), c.getFp());
		}
		

		//System.out.println(graph);
		System.out.format("Grafo caricato con %d vertici e %d archi", this.graph.vertexSet().size(), this.graph.edgeSet().size());
	}	
	//test
	public static void main(String args[]) {
		Model m= new Model();
	}
	
	
}
