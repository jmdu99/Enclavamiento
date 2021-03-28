//Grupo: Jose Manuel Diaz Urraco (170085), Daniel Lobato Navacerrada (170130)

import es.upm.babel.cclib.Monitor;
import es.upm.aedlib.priorityqueue.PriorityQueue;
import es.upm.aedlib.priorityqueue.SortedListPriorityQueue;

public class EnclavamientoMonitor implements Enclavamiento{
	// Atributos para el recurso
	private boolean presencia;
	private int[] tren;
	private Control.Color[] color;
	// Monitores y Condiciones
	private Monitor mutex;
	private Monitor.Cond cBarrera;
	private Monitor.Cond cFreno;
	private Monitor.Cond cSemaforo1;
	private Monitor.Cond cSemaforo2;
	private Monitor.Cond cSemaforo3;
	private PriorityQueue<Boolean,Monitor.Cond> colaBarrera;
	private PriorityQueue<Boolean,Monitor.Cond> colaFreno;
	private PriorityQueue<Control.Color,Monitor.Cond> colaSemaforo1;
	private PriorityQueue<Control.Color,Monitor.Cond> colaSemaforo2;
	private PriorityQueue<Control.Color,Monitor.Cond> colaSemaforo3;
	public EnclavamientoMonitor(){
		// Inicializamos el monitor y las condiciones de los monitores
		mutex = new Monitor();
		cBarrera = mutex.newCond();
		cFreno = mutex.newCond();
		cSemaforo1 = mutex.newCond();
		cSemaforo2 = mutex.newCond();
		cSemaforo3 = mutex.newCond();
		// Inicializamos los atributos
		this.presencia=false;
		tren = new int[4];
		color= new Control.Color[4];
		// Bucle tren y color
		for(int i = 0; i < tren.length; i++) {
			tren[i]=0;
			color[i]=Control.Color.VERDE;
		}
		colaBarrera=new SortedListPriorityQueue<Boolean, Monitor.Cond>();
		colaFreno=new SortedListPriorityQueue<Boolean, Monitor.Cond>();
		colaSemaforo1=new SortedListPriorityQueue<Control.Color, Monitor.Cond>();
		colaSemaforo2=new SortedListPriorityQueue<Control.Color, Monitor.Cond>();
		colaSemaforo3=new SortedListPriorityQueue<Control.Color, Monitor.Cond>();
	}
	//metodo auxiliar para hallar los colores correctos
	private void coloresCorrectos(){
		if(this.tren[1]>0) {
			this.color[1]=Control.Color.ROJO;
		}
		if(this.tren[1]==0 && (this.presencia ||this.tren[2]>0 )) {
			this.color[1]=Control.Color.AMARILLO;
		}
		if(this.tren[1]==0 && !this.presencia && this.tren[2]==0 ) {
			this.color[1]=Control.Color.VERDE;
		}
		if(this.tren[2]>0 || this.presencia) {
			this.color[2]=Control.Color.ROJO;
		}
		if(this.tren[2]==0 && !this.presencia) {
			this.color[2]=Control.Color.VERDE;
		}
		this.color[3]=Control.Color.VERDE;
	}

	//metodo auxiliar para desbloquear procesos
	private void desbloquear() {
		//condcBarrera
		if(cBarrera.waiting()>0  && !this.colaBarrera.first().getKey().equals(this.tren[1]+this.tren[2]==0)) {
			cBarrera.signal();
		}	
		//condcFreno
		else if(cFreno.waiting()>0  && !this.colaFreno.first().getKey().equals(this.tren[1]>1 || this.tren[2]>1 || this.tren[2]==1 && presencia)) {
			cFreno.signal();
		}
		//condcSemaforo1
		else if(cSemaforo1.waiting()>0  && this.colaSemaforo1.first().getKey()!=this.color[1]) {
			cSemaforo1.signal();
		}
		//condcSemaforo2
		else if(cSemaforo2.waiting()>0  && this.colaSemaforo2.first().getKey()!=this.color[2]) {
			cSemaforo2.signal();
		}
		//condcSemaforo3
		else if(cSemaforo3.waiting()>0  && this.colaSemaforo3.first().getKey()!=this.color[3]) {
			cSemaforo3.signal();
		}
	}
	@Override
	public void avisarPresencia(boolean presencia) {
		// Cogemos Mutex
		mutex.enter();
		// chequeo de la PRE
		// chequeo de la CPRE y posible bloqueo
		// implementacion de la POST
		this.presencia=presencia;
		this.coloresCorrectos();
		//desbloqueo
		this.desbloquear();
		// Liberamos Mutex
		mutex.leave();
	}

	@Override
	public boolean leerCambioBarrera(boolean actual) {
		// Cogemos Mutex
		mutex.enter();
		boolean esperado=false;
		// chequeo de la PRE
		// chequeo de la CPRE y posible bloqueo
		if(actual==(this.tren[1]+this.tren[2] ==0)) {
			this.colaBarrera.enqueue(actual, cBarrera);
			cBarrera.await();
			this.colaBarrera.dequeue();
		}
		// implementacion de la POST
		if(this.tren[1]+this.tren[2] ==0) {
			esperado=true;
		}
		// codigo de desbloqueo
		this.desbloquear();
		// Liberamos Mutex
		mutex.leave();
		return esperado;
	}

	@Override
	public boolean leerCambioFreno(boolean actual) {
		// Cogemos Mutex
		mutex.enter();
		boolean esperado=false;
		// chequeo de la PRE
		// chequeo de la CPRE y posible bloqueo
		if(actual==(this.tren[1]>1 || this.tren[2]>1 || this.tren[2]==1 && presencia)) {
			this.colaFreno.enqueue(actual, cFreno);
			cFreno.await();
			this.colaFreno.dequeue();
		}
		// implementacion de la POST
		if(this.tren[1]>1 || this.tren[2]>1 || this.tren[2]==1 && presencia) {
			esperado=true;
		}
		// codigo de desbloqueo
		this.desbloquear();
		// Liberamos Mutex
		mutex.leave();
		return esperado;
	}

	@Override
	public Control.Color leerCambioSemaforo(int i, Control.Color actual)throws PreconditionFailedException {
		// chequeo de la PRE
		if(i==0){
			throw new PreconditionFailedException();
		}
		// Cogemos Mutex
		mutex.enter();
		Control.Color esperado;
		// chequeo de la CPRE y posible bloqueo
		if(actual==this.color[i]) {
			//Semaforo1
			if(i==1) {
				this.colaSemaforo1.enqueue(actual, cSemaforo1);
				cSemaforo1.await();
				this.colaSemaforo1.dequeue();
			}
			//Semaforo2
			else if(i==2) {
				this.colaSemaforo2.enqueue(actual, cSemaforo2);
				cSemaforo2.await();
				this.colaSemaforo2.dequeue();
			}
			//Semaforo3
			else if(i==3) {
				this.colaSemaforo3.enqueue(actual, cSemaforo3);
				cSemaforo3.await();
				this.colaSemaforo3.dequeue();
			}
		}
		// implementacion de la POST
		esperado=this.color[i];
		// codigo de desbloqueo
		this.desbloquear();
		// Liberamos Mutex
		mutex.leave();
		return esperado;
	}

	@Override
	public void avisarPasoPorBaliza(int i) throws PreconditionFailedException  {
		// chequeo de la PRE;
		if(i==0){
			throw new PreconditionFailedException();
		}
		// Cogemos Mutex
		mutex.enter();
		// chequeo de la CPRE y posible bloqueo
		// implementacion de la POST
		this.tren[i-1]-=1;
		this.tren[i]+=1;
		this.coloresCorrectos();
		//desbloqueo
		this.desbloquear();
		// Liberamos Mutex
		mutex.leave();
	}
}
