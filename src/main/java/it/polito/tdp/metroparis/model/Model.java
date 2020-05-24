package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

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
		System.out.format("Grafo caricato con %d vertici e %d archi \n", this.graph.vertexSet().size(), this.graph.edgeSet().size());
	}
	
	//metodo per visitare grafo in ampiezza a partire da un certo vertice
	/**
	 * vogliamo visitare l'intero grafo con la strategia Breadth First e ritorna l'insieme dei vertici incontrati
	 * @param source vertice di partenza della visita
	 * @return insieme di vertici incontrati
	 */
	public List<Fermata> visitaAmpiezza(Fermata source) {
		List<Fermata> visita= new ArrayList<Fermata>();
		
		GraphIterator<Fermata, DefaultEdge> bfv= new BreadthFirstIterator<>(graph, source);
		//iteratore si posiziona sul primo elemento e posso andare avanti chiedendomi se esiste un elemento successivo
		while(bfv.hasNext()) {
			visita.add(bfv.next());
		}
		
		return visita;
	}
	
	//creiamo l'albero di visita in ampiezza (per semplicità)
	public Map<Fermata, Fermata> alberoVisita(Fermata source) {
		Map<Fermata, Fermata> albero= new HashMap<>();
		albero.put(source, null);
		
		GraphIterator<Fermata, DefaultEdge> bfv= new BreadthFirstIterator<>(graph, source);
		
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>(){
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) {
				//la visita sta considerando un nuovo arco
				//questo arco ha scoperto un nuovo vertice? 
				//Se si, provenendo da dove?
				
				//arco che l'algoritmo di visita sta attraversando
				DefaultEdge edge= e.getEdge(); //arc di tipo (a,b): ho scoperto a partendo da b, oppure ho scoperto b da a				
				//ora mi chiedo, a lo conoscevo già? era già stato visitato?
				Fermata a= graph.getEdgeSource(edge);
				Fermata b= graph.getEdgeTarget(edge);
				//se ad esempio a lo conosco già vuol dire che è già nelle chiavi della mappa
				if(albero.containsKey(a) && !albero.containsKey(b)) {
					//a è già noto e quindi ho scoperto b partendo da a
					albero.put(b,a);
				}
				else if (albero.containsKey(b) && !albero.containsKey(a)){
					//b è già noto e quindi ho scoperto a partendo da b
					albero.put(a,b);
				} //prima di questa iterazione inserisco a mano la sorgente (che non ha padre)
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {
			}
			});
		
		while(bfv.hasNext()) {
			bfv.next(); //estrai l'elemento e ignoralo (non lo salvo, ma so che attraversa arco che mi serve)
		}
			
		return albero;

	}
	
	//visita in profondita
	public List<Fermata> visitaProfondita(Fermata source) {
		List<Fermata> visita= new ArrayList<Fermata>();
		
		GraphIterator<Fermata, DefaultEdge> bfv= new DepthFirstIterator<>(graph, source);
		//iteratore si posiziona sul primo elemento e posso andare avanti chiedendomi se esiste un elemento successivo
		while(bfv.hasNext()) {
			visita.add(bfv.next());
		}
			
			return visita;
		}
		
	//cammini minimi
	public List<Fermata> camminiMinimi(Fermata partenza, Fermata arrivo) {
		DijkstraShortestPath<Fermata, DefaultEdge> dij= new DijkstraShortestPath<>(graph);
		
		GraphPath<Fermata, DefaultEdge> cammino = dij.getPath(partenza, arrivo);
		
		return cammino.getVertexList();
	}
	
	//test
	public static void main(String args[]) {
		Model m= new Model();
		List<Fermata> visita= m.visitaAmpiezza(m.fermate.get(0)); //--> prendo vertice sorgente
		System.out.println(visita);
		List<Fermata> visita2=m.visitaProfondita(m.fermate.get(0));
		System.out.println(visita2);
		
		Map<Fermata, Fermata> albero= m.alberoVisita(m.fermate.get(0));
		for(Fermata f: albero.keySet()) {
			System.out.format("%s <- %s", f, albero.get(f)+"\n");
		}
		
		List<Fermata> cammino= m.camminiMinimi(m.fermate.get(0), m.fermate.get(1));
		System.out.println(cammino);
	}
	
	
}
