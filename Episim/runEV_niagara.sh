#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=80
#SBATCH --ntasks=1
#SBATCH --time=1:00:00
#SBATCH --job-name=urban_ev_scenario
#SBATCH --account=def-fciari
#SBATCH --mail-user=yonsorena.nong@polymtl.ca
#SBATCH --mail-type=ALL
#SBATCH --output=/scratch/f/fciari/ashraf37/episimScenarioGenHK_22_06_2022.slurm


echo "Current working directory: `pwd`"
echo "Starting run at: `date`"

#Load environment
module load CCEnv arch/avx512 StdEnv/2020

# Load Java
module load java/14.0.2

# Load script
java -Xmx180G -cp Episim-0.0.1-SNAPSHOT.jar HKScenario.ReadAndChangeEventFile

###############################################


# ---------------------------------------------------------------------
echo "Job finished with exit code $? at: `date`"


