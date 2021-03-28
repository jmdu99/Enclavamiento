//Grupo: Jose Manuel Diaz Urraco (170085), Daniel Lobato Navacerrada (170130)

//importamos  las estructuras de datos necesarias

import java.util.LinkedList;
import java.util.Queue;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import org.jcsp.lang.ProcessManager;

/** 
 * Implementation using CSP (Mixed).
 * Tecnica de peticiones aplazadas 
 * excepto en las ops. de aviso (no bloqueantes)
 *
 * @author rul0
 */ 
public class EnclavamientoCSP implements CSProcess, Enclavamiento {

	/** WRAPPER IMPLEMENTATION */
	//** Channels for receiving external requests
	// Un canal por op. del recurso
	private  final Any2OneChannel chAvisarPresencia     = Channel.any2one();
	private  final Any2OneChannel chLeerCambioBarrera   = Channel.any2one();
	private  final Any2OneChannel chLeerCambioFreno     = Channel.any2one();
	private  final Any2OneChannel chLeerCambioSemaforo  = Channel.any2one();
	private  final Any2OneChannel chAvisarPasoPorBaliza = Channel.any2one();

	public EnclavamientoCSP() {
		new ProcessManager(this).start();
	}
	// Clases auxiliares para las peticiones que se envian al servidor
	public class PeticionLeerCambioBarrera{
		protected One2OneChannel channel;
		protected boolean value;

		public PeticionLeerCambioBarrera(One2OneChannel channel, boolean value) {
			this.channel = channel;
			this.value = value;
		} 
	}

	public class PeticionLeerCambioFreno{
		protected One2OneChannel channel;
		protected boolean value;

		public PeticionLeerCambioFreno(One2OneChannel channel, boolean value) {
			this.channel = channel;
			this.value = value;
		}
	}

	public class PeticionLeerCambioSemaforo{
		protected One2OneChannel channel;
		protected Control.Color color;
		protected int index;

		public PeticionLeerCambioSemaforo(One2OneChannel channel,
				Control.Color color,
				int index) {
			this.channel = channel;
			this.color = color;
			this.index = index;
		}
	}

	// Implementacion de la interfaz Enclavamiento
	@Override
	public void avisarPresencia(boolean presencia) {
		chAvisarPresencia.out().write(presencia);
	}

	@Override
	public boolean leerCambioBarrera(boolean abierta) {
		One2OneChannel ch = Channel.one2one();
		chLeerCambioBarrera.out().write(new PeticionLeerCambioBarrera(ch, abierta));

		return (Boolean) ch.in().read();
	}

	@Override
	public boolean leerCambioFreno(boolean accionado) {
		One2OneChannel ch = Channel.one2one();
		chLeerCambioFreno.out().write(new PeticionLeerCambioFreno(ch, accionado));

		return (Boolean) ch.in().read();
	}

	/** notice that exceptions can be thrown outside the server */
	@Override
	public Control.Color leerCambioSemaforo(int i, Control.Color color) {
		if (i == 0 )
			throw new PreconditionFailedException("Semaforo 0 no existe");

		One2OneChannel ch = Channel.one2one();
		chLeerCambioSemaforo.out().write(new PeticionLeerCambioSemaforo(ch, color, i));

		return (Control.Color) ch.in().read();
	}

	@Override
	public void avisarPasoPorBaliza(int i) {
		if (i == 0 )
			throw new PreconditionFailedException("Baliza 0 no existe");

		chAvisarPasoPorBaliza.out().write(i);
	}


	/** SERVER IMPLEMENTATION */
	static final int AVISAR_PRESENCIA = 0;
	static final int LEER_CAMBIO_BARRERA = 1;
	static final int LEER_CAMBIO_FRENO  = 2;
	static final int LEER_CAMBIO_SEMAFORO  = 3;
	static final int AVISAR_PASO_POR_BALIZA = 4;

	@Override
	public void run() {
		//estado del recurso: presencia, tren y color
		boolean presencia;
		int[] tren;
		Control.Color[] color;

		presencia=false;
		tren = new int[4];
		color= new Control.Color[4];

		for(int i = 0; i < tren.length; i++) {
			tren[i]=0;
			color[i]=Control.Color.VERDE;
		}

		// estructuras auxiliares para guardar
		// las peticiones aplazadas en el servidor

		Queue<PeticionLeerCambioBarrera> pendBarrera = new LinkedList<PeticionLeerCambioBarrera>();
		Queue<PeticionLeerCambioFreno> pendFreno = new LinkedList<PeticionLeerCambioFreno>();
		Queue<PeticionLeerCambioSemaforo> pendSemaforo = new LinkedList<PeticionLeerCambioSemaforo>();

		// Construccion de la recepcion alternativa
		Guard[] inputs = {
				chAvisarPresencia.in(),
				chLeerCambioBarrera.in(),
				chLeerCambioFreno.in(),
				chLeerCambioSemaforo.in(),
				chAvisarPasoPorBaliza.in()
		};

		Alternative services = new Alternative(inputs);
		int chosenService = 0;
		while (true){      
			chosenService = services.fairSelect();
			switch (chosenService) {

			case AVISAR_PRESENCIA:
				presencia=(Boolean)this.chAvisarPresencia.in().read();
				EnclavamientoCSP.coloresCorrectos(presencia, tren, color);
				break;

			case LEER_CAMBIO_BARRERA:
				PeticionLeerCambioBarrera petBarrera=(PeticionLeerCambioBarrera)this.chLeerCambioBarrera.in().read();
				pendBarrera.add(petBarrera);
				break;

			case LEER_CAMBIO_FRENO:
				PeticionLeerCambioFreno petFreno=(PeticionLeerCambioFreno)this.chLeerCambioFreno.in().read();
				pendFreno.add(petFreno);
				break;

			case LEER_CAMBIO_SEMAFORO:
				PeticionLeerCambioSemaforo petSemaforo=(PeticionLeerCambioSemaforo)this.chLeerCambioSemaforo.in().read();
				pendSemaforo.add(petSemaforo);
				break;

			case AVISAR_PASO_POR_BALIZA:
				int i =(Integer)this.chAvisarPasoPorBaliza.in().read();
				tren[i-1]-=1;
				tren[i]+=1;
				EnclavamientoCSP.coloresCorrectos(presencia, tren, color);
				break;
			} 

			//codigo de desbloqueo
			boolean esCambiado=true;
			while(esCambiado) {
				esCambiado=false;
				for(int i=0;i<pendBarrera.size();i++) {
					PeticionLeerCambioBarrera petBarrera=pendBarrera.poll();
					boolean esperado;
					if(petBarrera.value!=(tren[1]+tren[2] ==0)) {
						esperado=true;
						petBarrera.channel.out().write(esperado);
						esCambiado=true;
					}
					else {
						pendBarrera.add(petBarrera);
					}
				}

				for(int i=0;i<pendFreno.size();i++) {
					PeticionLeerCambioFreno petFreno=pendFreno.poll();
					boolean esperado;
					if(petFreno.value!=(tren[1]>1 || tren[2]>1 || tren[2]==1 && presencia)) {
						esperado=true;
						petFreno.channel.out().write(esperado);
						esCambiado=true;
					}
					else {
						pendFreno.add(petFreno);
					}
				}

				for(int i=0;i<pendSemaforo.size();i++) {
					PeticionLeerCambioSemaforo petSemaforo=pendSemaforo.poll();
					Control.Color esperado;
					if(petSemaforo.color!=color[petSemaforo.index]) {
						esperado=color[petSemaforo.index];
						petSemaforo.channel.out().write(esperado);
						esCambiado=true;
					}
					else {
						pendSemaforo.add(petSemaforo);
					}
				}
			}
		} 
	} 

	//metodos auxiliares
	private static void coloresCorrectos( boolean presencia,int[] tren, Control.Color[] color){
		if(tren[1]>0) {
			color[1]=Control.Color.ROJO;
		}
		if(tren[1]==0 && (presencia ||tren[2]>0 )) {
			color[1]=Control.Color.AMARILLO;
		}
		if(tren[1]==0 && !presencia && tren[2]==0 ) {
			color[1]=Control.Color.VERDE;
		}
		if(tren[2]>0 || presencia) {
			color[2]=Control.Color.ROJO;
		}
		if(tren[2]==0 && !presencia) {
			color[2]=Control.Color.VERDE;
		}
		color[3]=Control.Color.VERDE;
	}
}
